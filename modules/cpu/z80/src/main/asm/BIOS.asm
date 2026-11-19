; Primitives ROM (really RAM for now - no write protection exists on the
; Z80 side yet): raw hardware access - console, disk, and now graphics
; I/O - no command parsing or filesystem concepts, same split as the 6502
; side's own BIOS.asm/MONITOR.asm (which this file is deliberately modeled
; on).
;
; Calling convention:
;   BIOS_PUTCH         A = char to print; comes back holding the same
;                       character. BC/DE/HL are preserved - only A (and
;                       the flags) change - so a caller can keep a string
;                       pointer in HL across a print loop without having
;                       to save/restore it itself. (A real bug, not a
;                       hypothetical: Demo.asm's own PRINT_LOOP does
;                       exactly this, and broke the first time this
;                       wasn't true - every primitive below preserves
;                       BC/DE/HL for the same reason, even where nothing
;                       exercises it yet.)
;   BIOS_GETCH          blocks until a key is available; returns A = char
;   BIOS_VIDEO_INIT      no parameters - one-time setup
;   BIOS_GOTOXY          A = row (0-23), E = col (0-79)
;   BIOS_SELECT          A = drive (0-7)
;   BIOS_READ_SECTOR     A = track, E = sector, HL = destination buffer
;   BIOS_WRITE_SECTOR    A = track, E = sector, HL = source buffer
;   BIOS_FORMAT_TRACK    A = track, E = fill byte (one track only - a
;                        caller wanting the whole disk formatted loops
;                        over every track itself)
;   BIOS_RESTORE         no parameters - homes the selected drive's head
;                        back to track 0
;   BIOS_GRAPHICS_MODE   A = MODE_TEXT or MODE_GRAPHICS - just a register
;                        poke, VideoController itself decides how to paint
;                        based on it
;   BIOS_SET_PIXEL       A = row (0-199), E = on/off (0 = off), HL = column
;                        (0-319)
;
; Deliberately simpler than the 6502 version in one way: no hardware
; scrolling via Display Start Address - once output reaches the last row
; it just wraps back to row 0 rather than scrolling the window up. Good
; enough to prove the pipeline (CPU -> port bus -> VideoController ->
; screen) actually works; real scrolling is a reasonable follow-up once
; it is.
;
; Also simpler in a second way real Z80 hardware itself affords: HL is a
; genuine 16-bit pointer register, so BIOS_READ_SECTOR/BIOS_WRITE_SECTOR
; take their buffer pointer directly in HL - no need for the 6502
; version's zero-page ADDRLO/ADDRHI staging trick at this level. The
; shared C side (bios.h/bios_glue.asm, see modules/cpu/c-shared) still
; presents the same "stage one value, then trigger" single-parameter
; calls bios.h's cc65 heritage requires - that staging is bios_glue.asm's
; own private scratch, not a BIOS.asm-level concept, exactly because this
; primitive doesn't need one of its own.
;
; Every port access here goes through the ED-prefixed "IN A,(C)"/
; "OUT (C),A" forms (BC = port number, loaded fresh immediately before
; each use), never the unprefixed "IN A,(n)"/"OUT (n),A" forms - those
; put A itself on the upper byte of the real port address ((A<<8)|n, a
; real documented Z80 quirk our own emulator faithfully reproduces, see
; InAFromPort/OutAToPort), which is actively wrong for small port numbers
; like ours whenever A is carrying real data rather than 0.

VIDEO_ADDR equ $00
VIDEO_DATA equ $01
VIDEO_KBD_DATA equ $02
VIDEO_KBD_STAT equ $03
DATA_AVAILABLE equ $01

; FloppyController's own register layout (modules/Disk/.../FloppyController.java)
; registered on the Z80 machine's port bus at $10-$14 (see Z80VideoDiskMachine).
FD_STATUS equ $10
FD_TRACK  equ $11
FD_SECTOR equ $12
FD_DATA   equ $13
FD_DRIVE  equ $14

; VideoController's Mode Control register, same port range ($00-$04) as the
; other video registers above - $04 was always part of that range, just
; unused until now.
VIDEO_MODE equ $04

; 320x200 1bpp bitmap - same address and geometry as the 6502 side's own
; GRAPHICS_RAM, for easy cross-CPU comparison. See DO_SET_PIXEL below for
; the byte/bit addressing (row-major, MSB-first, matching
; VideoController.getPixel()).
GRAPHICS_RAM equ $C000
GRAPHICS_BYTES_PER_ROW equ 40

VR_H_DISPLAYED equ 1
VR_V_DISPLAYED equ 6
VR_CURSOR_START equ 10
VR_CURSOR_ADDR_HI equ 14
VR_CURSOR_ADDR_LO equ 15
CURSOR_ON equ $00

VIDEO_RAM equ $B800
V_COLS equ 80
V_ROWS equ 24

	org $F000

; ---- jump table: fixed, frozen entry points. A future caller (today,
;      Demo.asm; eventually a real monitor) calls these by address; moving
;      an implementation below is always safe as long as its slot here
;      still JPs to the right place. ----
	jp DO_PUTCH		; $F000 BIOS_PUTCH
	jp DO_GETCH		; $F003 BIOS_GETCH
	jp DO_VIDEO_INIT	; $F006 BIOS_VIDEO_INIT
	jp DO_GOTOXY		; $F009 BIOS_GOTOXY
	jp DO_SELECT		; $F00C BIOS_SELECT
	jp DO_READ_SECTOR	; $F00F BIOS_READ_SECTOR
	jp DO_WRITE_SECTOR	; $F012 BIOS_WRITE_SECTOR
	jp DO_FORMAT_TRACK	; $F015 BIOS_FORMAT_TRACK
	jp DO_RESTORE		; $F018 BIOS_RESTORE
	jp DO_GRAPHICS_MODE	; $F01B BIOS_GRAPHICS_MODE
	jp DO_SET_PIXEL		; $F01E BIOS_SET_PIXEL

; ---- A = char to print. CR moves to column 0 on the same row; LF moves
;      to column 0 on the next row (wrapping row at V_ROWS back to 0); any
;      other byte is poked at the current (row, col) and the cursor
;      advances, wrapping column at V_COLS into a row advance exactly like
;      LF's own row-wrap. ----
DO_PUTCH push bc
	push de
	push hl
	cp $0D
	jp z,PC_CR_ENTRY
	cp $0A
	jp z,PC_LF_ENTRY
	push af
	call COMPUTE_OFFSET	; hl = row*V_COLS + col
	ld de,VIDEO_RAM
	add hl,de
	pop af
	ld (hl),a
	call PC_ADVANCE
	jp PC_DONE
PC_CR_ENTRY push af
	call PC_CR
	pop af
	jp PC_DONE
PC_LF_ENTRY push af
	call PC_LF
	pop af
	;; falls through to PC_DONE
PC_DONE	pop hl
	pop de
	pop bc
	ret

; ---- hl = V_ROW*V_COLS + V_COL - the offset from VIDEO_RAM's own base
;      both the character-poke address and the Cursor Address register
;      use. A plain repeated-add loop (V_ROW is at most V_ROWS-1 = 23) -
;      this runs once per character, never once per pixel, so simplicity
;      wins over speed, same reasoning the 6502 side's own DO_GOTOXY used
;      for its row*V_COLS. ----
COMPUTE_OFFSET ld hl,0
	ld a,(V_ROW)
	ld b,a
CO_LOOP	ld a,b
	or 0
	jp z,CO_DONE
	ld de,V_COLS
	add hl,de
	dec b
	jp CO_LOOP
CO_DONE	ld a,(V_COL)
	ld e,a
	ld d,0
	add hl,de
	ret

; ---- advances the cursor by one column, wrapping into PC_NEXT_ROW at
;      V_COLS - CP's carry flag is unsigned "less than", so "jp c" after
;      "cp V_COLS" means "the incremented column is still in range, no
;      wrap needed." ----
PC_ADVANCE ld a,(V_COL)
	inc a
	cp V_COLS
	jp c,PA_STORE
	call PC_NEXT_ROW
	ld a,0
PA_STORE ld (V_COL),a
	jp PC_SET_CURSOR

; ---- advances V_ROW by one, wrapping back to row 0 at V_ROWS - no
;      scrolling, see this file's own header comment. ----
PC_NEXT_ROW ld a,(V_ROW)
	inc a
	cp V_ROWS
	jp c,PNR_STORE
	ld a,0
PNR_STORE ld (V_ROW),a
	ret

PC_CR	xor a
	ld (V_COL),a
	jp PC_SET_CURSOR

PC_LF	call PC_NEXT_ROW
	xor a
	ld (V_COL),a
	jp PC_SET_CURSOR

; ---- stages the Cursor Address register (R14/R15) from COMPUTE_OFFSET's
;      own result - the offset from VIDEO_RAM, same convention
;      VideoController's own Cursor Address doc already uses. ----
PC_SET_CURSOR call COMPUTE_OFFSET
	ld a,VR_CURSOR_ADDR_LO
	ld bc,VIDEO_ADDR
	out (c),a
	ld a,l
	ld bc,VIDEO_DATA
	out (c),a
	ld a,VR_CURSOR_ADDR_HI
	ld bc,VIDEO_ADDR
	out (c),a
	ld a,h
	ld bc,VIDEO_DATA
	out (c),a
	ret

; ---- one-time setup: configures the registers DO_PUTCH's own output
;      relies on, clears the visible screen (otherwise it would show
;      whatever garbage already happens to be in VIDEO_RAM), and resets
;      this file's own row/column tracking to match. ----
DO_VIDEO_INIT push bc
	push de
	push hl
	ld a,VR_H_DISPLAYED
	ld bc,VIDEO_ADDR
	out (c),a
	ld a,V_COLS
	ld bc,VIDEO_DATA
	out (c),a
	ld a,VR_V_DISPLAYED
	ld bc,VIDEO_ADDR
	out (c),a
	ld a,V_ROWS
	ld bc,VIDEO_DATA
	out (c),a
	ld a,VR_CURSOR_START
	ld bc,VIDEO_ADDR
	out (c),a
	ld a,CURSOR_ON
	ld bc,VIDEO_DATA
	out (c),a
	xor a
	ld (V_ROW),a
	ld (V_COL),a
	ld hl,VIDEO_RAM
	ld bc,V_ROWS*V_COLS
VI_CLEAR_LOOP ld (hl),' '
	inc hl
	dec bc
	ld a,b
	or c
	jp nz,VI_CLEAR_LOOP
	call PC_SET_CURSOR
	pop hl
	pop de
	pop bc
	ret

; ---- A = row, E = col. Repoints V_ROW/V_COL (so a following BIOS_PUTCH -
;      and its own CR/LF/wrap handling - picks up from here exactly as if
;      it had advanced into this cell normally) and the hardware cursor. ----
DO_GOTOXY push bc
	push de
	push hl
	ld (V_ROW),a
	ld a,e
	ld (V_COL),a
	call PC_SET_CURSOR
	pop hl
	pop de
	pop bc
	ret

DO_GETCH push bc
DO_GETCH_POLL ld bc,VIDEO_KBD_STAT
	in a,(c)
	and DATA_AVAILABLE
	jp z,DO_GETCH_POLL
	ld bc,VIDEO_KBD_DATA
	in a,(c)
	pop bc
	ret

; ---- A = drive (0-7). ----
DO_SELECT push bc
	ld bc,FD_DRIVE
	out (c),a
	pop bc
	ret

; ---- A = track, E = sector, HL = destination buffer. The Read Sector
;      command loads its whole 128-byte buffer synchronously (this
;      emulator's own FloppyController has no real per-byte transfer
;      delay), so this can just drain FD_DATA in a tight loop - no
;      DRQ/interrupt handling needed for a simple polling reader. ----
DO_READ_SECTOR push bc
	push de
	push hl
	ld bc,FD_TRACK
	out (c),a
	ld bc,FD_SECTOR
	ld a,e
	out (c),a
	ld bc,FD_STATUS
	ld a,$80
	out (c),a
	ld d,128
DRS_LOOP ld bc,FD_DATA
	in a,(c)
	ld (hl),a
	inc hl
	dec d
	jp nz,DRS_LOOP
	pop hl
	pop de
	pop bc
	ret

; ---- A = track, E = sector, HL = source buffer. Mirrors DO_READ_SECTOR
;      in reverse - feeding FD_DATA drives the controller's Write Sector
;      transfer, which flushes to the backing file on the 128th byte. ----
DO_WRITE_SECTOR push bc
	push de
	push hl
	ld bc,FD_TRACK
	out (c),a
	ld bc,FD_SECTOR
	ld a,e
	out (c),a
	ld bc,FD_STATUS
	ld a,$a0
	out (c),a
	ld d,128
DWS_LOOP ld a,(hl)
	ld bc,FD_DATA
	out (c),a
	inc hl
	dec d
	jp nz,DWS_LOOP
	pop hl
	pop de
	pop bc
	ret

; ---- A = track, E = fill byte. One track only, like a real WD179x Write
;      Track - a caller wanting the whole disk formatted loops over every
;      track itself. Runs to completion synchronously, like Seek/Restore,
;      so there's no real BUSY period to poll. ----
DO_FORMAT_TRACK push bc
	push de
	ld bc,FD_TRACK
	out (c),a
	ld bc,FD_DATA
	ld a,e
	out (c),a
	ld bc,FD_STATUS
	ld a,$f0
	out (c),a
	pop de
	pop bc
	ret

; ---- home the selected drive's head to track 0. No parameters - restore
;      doesn't need a target track like seek does. ----
DO_RESTORE push bc
	ld bc,FD_STATUS
	ld a,$00
	out (c),a
	pop bc
	ret

; ---- A = MODE_TEXT or MODE_GRAPHICS. Just a register poke -
;      VideoController itself decides how to paint based on it. ----
DO_GRAPHICS_MODE push bc
	ld bc,VIDEO_MODE
	out (c),a
	pop bc
	ret

; ---- A = row (0-199), E = on/off (0 = off, anything else = on), HL =
;      column (0-319). GRAPHICS_RAM is addressed row-major: byteOffset =
;      row*GRAPHICS_BYTES_PER_ROW + col/8, bit (7 - col mod 8) within that
;      byte - MSB-first, matching VideoController.getPixel() and the
;      6502 side's own DO_SET_PIXEL.
;
;      col mod 8 only ever needs L (col's low byte), same reasoning the
;      6502 version uses for ADDRLO - 8 divides 256 evenly, so the high
;      byte can't affect it. col/8 is a plain 16-bit logical shift right
;      by 3 (SRL H / RR L, three times - RR L picks up the bit SRL H just
;      shifted out, so this is one real 16-bit shift, not two independent
;      8-bit ones), and row*GRAPHICS_BYTES_PER_ROW is added on top via a
;      repeated-add loop, same style as COMPUTE_OFFSET's own row*V_COLS.
;      PIXEL_ROW/PIXEL_ONOFF/PIXEL_MASK are scratch bytes, same idea as
;      V_ROW/V_COL above, used here only because the real parameters
;      (A, E) would otherwise get clobbered by HL's own table-lookup/
;      shift/add work before they're needed again. ----
DO_SET_PIXEL push bc
	push de
	push hl
	ld (PIXEL_ROW),a
	ld a,e
	ld (PIXEL_ONOFF),a

	ld a,l
	and $07
	ld c,a
	ld b,0
	ld hl,PIXEL_BIT_MASKS
	add hl,bc
	ld a,(hl)
	ld (PIXEL_MASK),a

	pop hl			; hl = column again
	srl h
	rr l
	srl h
	rr l
	srl h
	rr l			; hl = column / 8

	ld a,(PIXEL_ROW)
	ld b,a
DSP_MUL_LOOP ld a,b
	or 0
	jp z,DSP_MUL_DONE
	ld de,GRAPHICS_BYTES_PER_ROW
	add hl,de
	dec b
	jp DSP_MUL_LOOP
DSP_MUL_DONE ld de,GRAPHICS_RAM
	add hl,de		; hl -> the one byte to read-modify-write

	ld a,(PIXEL_ONOFF)
	or 0
	jp z,DSP_CLEAR
	ld a,(hl)
	ld b,a
	ld a,(PIXEL_MASK)
	or b
	jp DSP_STORE
DSP_CLEAR ld a,(PIXEL_MASK)
	cpl
	ld b,a
	ld a,(hl)
	and b
DSP_STORE ld (hl),a
	pop de
	pop bc
	ret

PIXEL_BIT_MASKS defb $80,$40,$20,$10,$08,$04,$02,$01
PIXEL_ROW defb 0
PIXEL_ONOFF defb 0
PIXEL_MASK defb 0

; ---- BIOS's own row/column tracking for DO_PUTCH's output -
;      VideoController's hardware has no concept of "where the next
;      character goes" (that is always firmware's job, even on real
;      CRTC-based machines), so this file owns it. ----
V_ROW	defb 0
V_COL	defb 0
