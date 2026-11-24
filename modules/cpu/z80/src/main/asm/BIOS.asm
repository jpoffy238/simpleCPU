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
;   BIOS_GOTOXY          A = row (0-23, relative to the current scrolled
;                        window, same convention PC_SCROLL itself uses),
;                        E = col (0-79)
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
;   BIOS_GET_TIME        no parameters; HL = destination 4-byte buffer -
;                        copies RTC's Unix-epoch-seconds counter (RTC.java)
;                        there as a little-endian unsigned long (this
;                        project's own time_t; no calendar/display
;                        conversion - see RTC.java's own comment)
;
; Real hardware scrolling via Display Start Address (R12/R13), mirroring
; the 6502 side's own BIOS.asm - see DO_PUTCH/PC_SCROLL below. Simpler
; than the 6502 version in one way Z80 itself affords: V_ADDR/V_START are
; kept as plain 16-bit ring-buffer OFFSETS (0..VIDEO_RAM_SIZE-1, not
; absolute addresses), and VIDEO_RAM_SIZE being a power of two (2048)
; means wrapping after ANY add or subtract - overflow past the top AND
; underflow below zero alike - is just "mask the high byte to 3 bits"
; (see WRAP_RING), one shared step every path below runs through, rather
; than the 6502 version's own separate per-path overflow/underflow
; branches (a 6502 ADC/SBC only ever carries/borrows one bit at a time on
; an 8-bit register, so it has no equivalent single-step shortcut).
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

; VectoredInterruptController's one register (see Z80VideoDiskMachine) - the
; IM2 vector byte DO_READ_SECTOR/DO_WRITE_SECTOR program on first use (see
; DISK_IM2_INIT below). Nothing else on this machine's firmware touches
; interrupts at all.
INT_VECTOR equ $05

; RTC's own big-endian Unix-epoch-seconds register block (see RTC.java) -
; read-only, 4 bytes, registered on the port bus at $06-$09 (see
; Z80VideoDiskMachine) - free space between VectoredInterruptController's
; vector register ($05) and FloppyController's own registers ($10-$14).
RTC_TIME equ $06

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
VR_START_ADDR_HI equ 12
VR_START_ADDR_LO equ 13
VR_CURSOR_ADDR_HI equ 14
VR_CURSOR_ADDR_LO equ 15
CURSOR_ON equ $00

; VIDEO_RAM_SIZE is the whole ring PC_SCROLL/DO_GOTOXY scan via Display
; Start Address (R12/R13) - bigger than the 1920 bytes (V_COLS*V_ROWS)
; actually visible at once, so there's always at least one spare row to
; scroll a new line into by moving the window rather than copying every
; row up. Deliberately a power of two - see WRAP_RING.
VIDEO_RAM equ $B800
VIDEO_RAM_SIZE equ $0800
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
	jp DO_GET_TIME		; $F021 BIOS_GET_TIME

; ---- A = char to print. CR moves to column 0 on the same row; LF moves
;      to column 0 on the next row, scrolling once the cursor has stepped
;      V_ROWS rows below Display Start Address; BS moves one column left,
;      no wrap (matches cc65/ccp.c's own expectation - see read_line()'s
;      "\b, space, \b" visual-erase sequence, shared unchanged with the
;      6502 side); any other byte is poked at the current position and
;      the cursor advances, wrapping column at V_COLS into the same
;      row-crossing/scroll check LF's own row move runs through. ----
DO_PUTCH push bc
	push de
	push hl
	cp $0D
	jp z,PC_CR_ENTRY
	cp $0A
	jp z,PC_LF_ENTRY
	cp $08
	jp z,PC_BS_ENTRY
	push af
	ld hl,(V_ADDR)
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
	jp PC_DONE
PC_BS_ENTRY push af
	call PC_BS
	pop af
	;; falls through to PC_DONE
; ---- shared tail for every path above: stage the Cursor Address
; register from the (possibly just-updated) V_ADDR, then restore the
; original character into A before returning - PC_SET_CURSOR clobbers A
; internally (it's how it talks to the port registers), so this needs
; its own save/restore around that call, on top of each entry path's own
; save/restore around its own helper call above. ----
PC_DONE	push af
	call PC_SET_CURSOR
	pop af
	pop hl
	pop de
	pop bc
	ret

; ---- masks a 16-bit ring-buffer offset in HL down to 0..VIDEO_RAM_SIZE-1
;      - VIDEO_RAM_SIZE (2048 = $0800) being a power of two means this
;      correctly wraps both an overflow past the top AND an underflow
;      below zero (a negative offset's two's-complement bit pattern,
;      masked to the same low bits, lands exactly on the right positive
;      wrapped value) - one shared step every path below runs an add or
;      subtract through, rather than a separate overflow/underflow branch
;      per path. ----
WRAP_RING ld a,h
	and $07
	ld h,a
	ret

; ---- V_ADDR -= V_COL (back to the start of the current row); V_COL=0. ----
PC_CR	ld hl,(V_ADDR)
	ld a,(V_COL)
	ld e,a
	ld d,0
	or a
	sbc hl,de
	call WRAP_RING
	ld (V_ADDR),hl
	xor a
	ld (V_COL),a
	ret

; ---- V_ADDR += (V_COLS - V_COL) (distance to the start of the next
;      row); V_COL=0; then PC_CHECK_SCROLL, since this always crosses a
;      row boundary. ----
PC_LF	ld a,(V_COL)
	ld e,a
	ld d,0
	ld hl,V_COLS
	or a
	sbc hl,de
	ld de,(V_ADDR)
	add hl,de
	call WRAP_RING
	ld (V_ADDR),hl
	xor a
	ld (V_COL),a
	jp PC_CHECK_SCROLL

; ---- one column left, no wrap at V_COLS/row 0 - already at column 0
;      means there's nothing to do (matches ConsoleDevice's own BS, and
;      real dumb-terminal behavior generally). ----
PC_BS	ld a,(V_COL)
	or 0
	ret z
	dec a
	ld (V_COL),a
	ld hl,(V_ADDR)
	dec hl
	call WRAP_RING
	ld (V_ADDR),hl
	ret

; ---- advances V_COL by one, wrapping at V_COLS back to 0, and V_ADDR by
;      one, ring-wrapping via WRAP_RING - then PC_CHECK_SCROLL, since a
;      column wrap here means a row boundary was just crossed exactly
;      like PC_LF's own row move. ----
PC_ADVANCE ld a,(V_COL)
	inc a
	cp V_COLS
	jp c,PA_STORE
	xor a
PA_STORE ld (V_COL),a
	ld hl,(V_ADDR)
	inc hl
	call WRAP_RING
	ld (V_ADDR),hl
	jp PC_CHECK_SCROLL

; ---- scrolls once the cursor has stepped V_ROWS rows below Display
;      Start Address - V_COL is already exactly 0 right at the moment a
;      row boundary is crossed (both PC_ADVANCE's own wrap and PC_LF zero
;      it before getting here), and nowhere else, so it doubles as the
;      "was this call actually a row crossing" signal, same convention
;      the 6502 side's own PC_CHECK_SCROLL uses. ----
PC_CHECK_SCROLL ld a,(V_COL)
	or 0
	ret nz
	ld a,(V_ROW)
	inc a
	ld (V_ROW),a
	cp V_ROWS
	ret nz
	call PC_SCROLL
	ret

; ---- ring-buffer scroll: moves Display Start Address (R12/R13, shadowed
;      in V_START) forward by one row instead of copying every row above
;      it up. Blanks the one row V_ADDR now points at (the row this
;      scroll just revealed) a byte at a time - V_COLS (80) doesn't
;      divide VIDEO_RAM_SIZE (2048) evenly, so a row's 80 bytes can
;      straddle the ring's wraparound point, and each byte's own absolute
;      address has to be recomputed from the (already-wrapped) running
;      offset rather than just incremented past it - then pins V_ROW back
;      to V_ROWS-1: both Display Start Address and the cursor moved down
;      by one row together, so the cursor's row position relative to the
;      (new) window is unchanged - still the last visible row. ----
PC_SCROLL push bc
	push de
	push hl
	ld hl,(V_START)
	ld de,V_COLS
	add hl,de
	call WRAP_RING
	ld (V_START),hl
	ld a,VR_START_ADDR_LO
	ld bc,VIDEO_ADDR
	out (c),a
	ld a,l
	ld bc,VIDEO_DATA
	out (c),a
	ld a,VR_START_ADDR_HI
	ld bc,VIDEO_ADDR
	out (c),a
	ld a,h
	ld bc,VIDEO_DATA
	out (c),a

	ld hl,(V_ADDR)
	ld b,V_COLS
PS_BLANK_LOOP push hl
	ld de,VIDEO_RAM
	add hl,de
	ld (hl),' '
	pop hl
	inc hl
	call WRAP_RING
	dec b
	jp nz,PS_BLANK_LOOP

	ld a,V_ROWS-1
	ld (V_ROW),a

	pop hl
	pop de
	pop bc
	ret

; ---- stages the Cursor Address register (R14/R15) from V_ADDR directly -
;      V_ADDR is already the absolute ring offset (not relative to
;      Display Start Address), same convention VideoController's own
;      Cursor Address doc uses, so no COMPUTE_OFFSET-style recomputation
;      is needed here at all. ----
PC_SET_CURSOR ld hl,(V_ADDR)
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
;      whatever garbage already happens to be in VIDEO_RAM), resets
;      Display Start Address to 0, and resets this file's own
;      row/column/ring-offset tracking to match. Only the visible
;      V_ROWS*V_COLS cells are cleared, not the whole VIDEO_RAM_SIZE ring
;      - the "extra" scrollback area starts out as garbage, same as the
;      6502 side, but PC_SCROLL always blanks a row before it becomes
;      visible, so that garbage is never actually seen. ----
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
	ld a,VR_START_ADDR_LO
	ld bc,VIDEO_ADDR
	out (c),a
	xor a
	ld bc,VIDEO_DATA
	out (c),a
	ld a,VR_START_ADDR_HI
	ld bc,VIDEO_ADDR
	out (c),a
	xor a
	ld bc,VIDEO_DATA
	out (c),a
	ld hl,0
	ld (V_ADDR),hl
	ld (V_START),hl
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

; ---- A = row (relative to the current scrolled window - the same
;      convention PC_SCROLL/V_ROW use), E = col. Repoints V_ADDR (so a
;      following BIOS_PUTCH - and its own CR/LF/BS/scroll handling -
;      picks up from here exactly as if it had advanced into this cell
;      normally), V_ROW directly (without this, a gotoxy to a row other
;      than where V_ROW already thought the cursor was would leave
;      PC_CHECK_SCROLL's row count out of sync with reality, scrolling at
;      the wrong moment), V_COL, and the hardware cursor bar. row*V_COLS
;      is a plain repeated-add loop (row is at most V_ROWS-1 = 23) rather
;      than a shift-based multiply - this runs once per cursor move/
;      redraw, never once per character, so simplicity wins over speed,
;      same reasoning the 6502 side's own DO_GOTOXY uses. ----
DO_GOTOXY push bc
	push de
	push hl
	ld (V_ROW),a
	ld b,a
	ld a,e
	ld (V_COL),a
	ld hl,0
GX_ROW_LOOP ld a,b
	or 0
	jp z,GX_ROW_DONE
	ld de,V_COLS
	add hl,de
	dec b
	jp GX_ROW_LOOP
GX_ROW_DONE ld a,(V_COL)
	ld e,a
	ld d,0
	add hl,de
	ld de,(V_START)
	add hl,de
	call WRAP_RING
	ld (V_ADDR),hl
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

; ---- A = track, E = sector, HL = destination buffer. Used to just drain
;      FD_DATA in a tight 128-iteration loop, trusting that FloppyController
;      had already buffered the whole sector in Java before this even ran -
;      never a faithful model of real WD179x-era hardware, which transfers
;      one byte per DRQ/interrupt. Now genuinely interrupt-driven: this
;      issues the Read Sector command and waits (DRS_WAIT) while DISK_ISR,
;      a real IM2 handler, drains the bytes in the background - see this
;      file's own IM2 section below (DISK_IM2_INIT/DISK_ISR) for the whole
;      mechanism. External contract unchanged - still blocks until the
;      transfer is genuinely done, still preserves BC/DE/HL. ----
DO_READ_SECTOR push af
	push de
	push hl
	call DISK_IM2_INIT
	pop hl
	pop de
	pop af
	push bc
	push de
	push hl
	ld bc,FD_TRACK
	out (c),a
	ld bc,FD_SECTOR
	ld a,e
	out (c),a
	ld (DISK_BUFFER_PTR),hl
	ld a,128
	ld (DISK_BYTES_REMAINING),a
	xor a
	ld (DISK_TRANSFER_IS_WRITE),a
	ld (DISK_TRANSFER_DONE),a
	ld a,1
	ld (DISK_TRANSFER_ACTIVE),a
	ld bc,FD_STATUS
	ld a,$80
	out (c),a		; beginRead(): arms BUSY|DRQ, raises the "byte 0 ready" interrupt
DRS_WAIT ld a,(DISK_TRANSFER_DONE)
	or 0
	jp z,DRS_WAIT
	pop hl
	pop de
	pop bc
	ret

; ---- A = track, E = sector, HL = source buffer. Mirrors DO_READ_SECTOR's
;      own interrupt-driven design, with one real-hardware wrinkle: Write
;      Sector's own first byte is never announced by an interrupt
;      (FloppyController.beginWrite() raises none - "firmware already knows
;      to start writing once BUSY is set", see its own comment), so this
;      routine sends byte 0 itself, after staging all of DISK_ISR's
;      bookkeeping - the very act of sending it raises the "ready for byte
;      1" interrupt, which DISK_ISR must already be able to see armed. ----
DO_WRITE_SECTOR push af
	push de
	push hl
	call DISK_IM2_INIT
	pop hl
	pop de
	pop af
	push bc
	push de
	push hl
	ld bc,FD_TRACK
	out (c),a
	ld bc,FD_SECTOR
	ld a,e
	out (c),a
	ld bc,FD_STATUS
	ld a,$a0
	out (c),a		; beginWrite(): arms BUSY|DRQ, raises nothing yet
	push hl
	inc hl
	ld (DISK_BUFFER_PTR),hl	; -> byte 1, the next byte DISK_ISR will send
	ld a,127
	ld (DISK_BYTES_REMAINING),a
	ld a,1
	ld (DISK_TRANSFER_IS_WRITE),a
	ld (DISK_TRANSFER_ACTIVE),a
	xor a
	ld (DISK_TRANSFER_DONE),a
	pop hl			; hl -> byte 0 again
	ld a,(hl)
	ld bc,FD_DATA
	out (c),a		; sends byte 0 - arms the "ready for byte 1" interrupt;
				; all of DISK_ISR's bookkeeping above is already in place
DWS_WAIT ld a,(DISK_TRANSFER_DONE)
	or 0
	jp z,DWS_WAIT
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

; ---- BIOS's own row/column/ring-offset tracking for DO_PUTCH's output -
;      VideoController's hardware has no concept of "where the next
;      character goes" (that is always firmware's job, even on real
;      CRTC-based machines), so this file owns it. V_ADDR/V_START are
;      16-bit offsets from VIDEO_RAM (0..VIDEO_RAM_SIZE-1, see
;      WRAP_RING) - V_ADDR is the cursor's own ring position, V_START
;      shadows Display Start Address (R12/R13) so PC_SCROLL/DO_GOTOXY
;      never have to read a register back to know where the visible
;      window currently starts. V_ROW is how many rows below V_START the
;      cursor currently is (0..V_ROWS-1), not an absolute row - it's what
;      PC_CHECK_SCROLL actually compares against V_ROWS to decide when to
;      scroll. ----
V_ADDR	defw 0
V_START	defw 0
V_ROW	defb 0
V_COL	defb 0

; ---- no parameters; HL = destination 4-byte buffer. Copies RTC's raw
;      Unix-epoch-seconds counter (RTC.java) from the port bus, reversing
;      its big-endian register layout into little-endian byte order - the
;      native order this CPU (and C's own unsigned long) expects - rather
;      than a flat copy. Reading RTC_TIME itself first (not RTC_TIME+3)
;      deliberately triggers RTC's own read-time refresh (see RTC.read())
;      before the other three bytes are read, so all four come from one
;      consistent instant despite being four separate port reads. Same
;      "preserve BC/HL for the caller" convention as every other primitive
;      in this file, even though HL is also this call's own parameter. ----
DO_GET_TIME push bc
	push hl
	ld bc,RTC_TIME
	in a,(c)		; byte 0 (MSB) - triggers RTC's own refresh
	inc hl
	inc hl
	inc hl			; hl -> out+3 (LSB slot)
	ld (hl),a
	dec hl
	inc bc
	in a,(c)		; byte 1
	ld (hl),a
	dec hl
	inc bc
	in a,(c)		; byte 2
	ld (hl),a
	dec hl
	inc bc
	in a,(c)		; byte 3 (LSB)
	ld (hl),a
	pop hl
	pop bc
	ret

; ==== IM2: real interrupt-driven disk I/O =================================
;
; DISK_IM2_INIT (called once, lazily, by DO_READ_SECTOR/DO_WRITE_SECTOR on
; their first use - DISK_IM2_READY guards it) programs
; VectoredInterruptController's one vector register with DISK_ISR_VECTOR's
; own low byte, points I at its page (high byte), selects IM 2, and leaves
; interrupts enabled from here on. No matching DI anywhere - this machine's
; only other interrupt-raising paths (FloppyController's Restore/Seek/
; Format completion; RTC, whose own ticking Thread nothing here ever
; starts) either need no per-byte servicing or never actually fire, so
; there's nothing to protect firmware's normal flow from by ever turning
; interrupts back off. ----
DISK_IM2_INIT ld a,(DISK_IM2_READY)
	or 0
	ret nz
	push bc
	ld a,DISK_ISR_VECTOR&$ff
	ld bc,INT_VECTOR
	out (c),a
	ld a,DISK_ISR_VECTOR>>8
	ld i,a
	im 2
	ei
	ld a,1
	ld (DISK_IM2_READY),a
	pop bc
	ret

; ---- The real ISR: fires once per byte FloppyController raises ready
;      (see its own beginRead/readData/writeData), plus once for every
;      Restore/Seek/Format completion even though none of those need
;      per-byte servicing - DISK_TRANSFER_ACTIVE is what tells a real
;      in-progress transfer's interrupt apart from one of those stray
;      completions, which just fall straight through to RETI. Must save
;      and restore everything it touches: a real interrupt can land
;      between any two instructions of whatever BDOS/CCP/user code happens
;      to be running at the time, not just inside DO_READ_SECTOR/
;      DO_WRITE_SECTOR's own wait loop. Re-arms with EI before RETI (real
;      Z80 hardware clears both IFF1 and IFF2 on interrupt entry and RETI
;      itself never restores them - the handler has to) - this is also
;      exactly what lets the next byte's own interrupt, already raised as
;      a side effect of this same IN/OUT below, actually get serviced
;      immediately after this one returns, without ever waiting on
;      DRS_WAIT/DWS_WAIT's own polling to notice. ----
DISK_ISR push af
	push bc
	push hl
	ld a,(DISK_TRANSFER_ACTIVE)
	or 0
	jp z,DISK_ISR_DONE
	ld hl,(DISK_BUFFER_PTR)
	ld a,(DISK_TRANSFER_IS_WRITE)
	or 0
	jp nz,DISK_ISR_WRITE
	ld bc,FD_DATA
	in a,(c)
	ld (hl),a
	jp DISK_ISR_ADVANCE
DISK_ISR_WRITE ld a,(hl)
	ld bc,FD_DATA
	out (c),a
DISK_ISR_ADVANCE inc hl
	ld (DISK_BUFFER_PTR),hl
	ld a,(DISK_BYTES_REMAINING)
	dec a
	ld (DISK_BYTES_REMAINING),a
	or 0
	jp nz,DISK_ISR_DONE
	xor a
	ld (DISK_TRANSFER_ACTIVE),a
	ld a,1
	ld (DISK_TRANSFER_DONE),a
DISK_ISR_DONE pop hl
	pop bc
	pop af
	ei
	reti

DISK_IM2_READY defb 0
DISK_BUFFER_PTR defw 0
DISK_BYTES_REMAINING defb 0
DISK_TRANSFER_ACTIVE defb 0
DISK_TRANSFER_DONE defb 0
DISK_TRANSFER_IS_WRITE defb 0
DISK_ISR_VECTOR defw DISK_ISR
