; A thin adapter presenting the REAL CP/M 2.2 BDOS calling convention (CALL
; 5, function number in C, parameter in E/DE, result in A/HL) in front of
; this project's own already-proven BIOS_PUTCH/BIOS_GETCH primitives
; (BIOS.asm) - not a parallel reimplementation of console I/O. This lets an
; unmodified, independently-compiled real CP/M 2.2 .COM binary run on this
; project's own Z80 core, which otherwise only understands this project's
; own custom BIOS_*/BDOS_* jump tables.
;
; Low-memory vectors ($0000 WBOOT, $0005 BDOS entry) are NOT set up by this
; file - a real CP/M system's own cold-boot loader would poke them once at
; boot time, and so does Test_CpmCompat.java, the same way every other
; hand-poked test fixture in this project already sets up its own entry
; points rather than needing a full loader.
;
; Phase 1: console-only functions - 0 (System Reset), 1 (Console Input), 2
; (Console Output), 9 (Print String), 11 (Get Console Status), 12 (Get
; Version Number).
;
; Phase 2 (this update): read-only disk functions - 14 (Select Disk), 15
; (Open File), 20 (Read Sequential), 25 (Get Current Disk), 26 (Set DMA) -
; delegating into the existing, already-proven BDOS_FIND_FILE/
; BDOS_READ_RECORD (bdos.c) rather than reimplementing directory/file
; logic, same reuse posture as Phase 1's console delegation. This adapter
; only ever tracks ONE open file at a time - matching bdos.c's own
; single-open-file limitation (its "currently open for reading" state is a
; plain C static, not per-caller) - a real program with two files open at
; once isn't supported by this phase. Write functions (Close/Delete/Write
; Sequential/Make File/Rename) remain a deliberate, separate next phase,
; not forgotten.
;
; Anything else returns A=$FF (an unimplemented-function error) rather than
; silently doing the wrong thing. A plain linear cp/jp-z chain, not a
; computed jump table - checked once per BDOS call (never once per
; character the way DO_PUTCH is), so there's no reason to prefer a denser
; but harder-to-verify dispatch over code that's easy to trace by hand.
;
; BC/DE/HL are NOT preserved across a call here, unlike this project's own
; BIOS.asm primitives - real CP/M's own BDOS never promised that either, and
; a real program calling real CP/M never assumes it.

BIOS_PUTCH equ $F000
BIOS_GETCH equ $F003
BIOS_SELECT equ $F00C
BDOS_FIND_FILE equ $A006
BDOS_READ_RECORD equ $A009

; VideoController's keyboard-status port (see Z80VideoDiskMachine - base
; $00, so this is a bare port number, not an offset).
VIDEO_KBD_STAT equ $03
DATA_AVAILABLE equ $01

; FloppyController's own Drive Select register (see Z80VideoDiskMachine -
; base $10, so +4 is a bare port number, not an offset).
FD_DRIVE equ $14

; bdos.c's own TRANSFER macro - a fixed, bare RAM address (NOT a
; linker-placed symbol), because bdos.c reads/writes this exact same
; address directly via a raw pointer cast (see bdos.c's own #define
; TRANSFER comment) - not something this adapter invented. LD (nn),HL/
; LD HL,(nn) write/read both bytes of a 16-bit pointer here in one
; instruction.
TRANSFER equ $0040

	org $E000

; ---- BDOS entry point - real CP/M's own CALL 5 convention lands here via
;      the JP a real cold-boot loader (or Test_CpmCompat.java, for now)
;      pokes at $0005. C = function number on entry. ----
BDOS_ENTRY ld a,c
	cp 0
	jp z,FN_RESET
	cp 1
	jp z,FN_CONIN
	cp 2
	jp z,FN_CONOUT
	cp 9
	jp z,FN_PRINT_STRING
	cp 11
	jp z,FN_CONST
	cp 12
	jp z,FN_VERSION
	cp 14
	jp z,FN_SELDSK
	cp 15
	jp z,FN_OPEN
	cp 20
	jp z,FN_READ_SEQ
	cp 25
	jp z,FN_CURDSK
	cp 26
	jp z,FN_SETDMA
FN_UNIMPLEMENTED ld a,$ff
	ret

; ---- Function 0: System Reset - no real warm-boot-from-disk reload exists
;      yet (no shell to reload in this phase), so this is a plain no-op. ----
FN_RESET ret

; ---- Function 1: Console Input - blocks for one character, returns it in
;      A, AND echoes it to the console - real CP/M's own function 1
;      genuinely does echo automatically (unlike BIOS_GETCH, which only
;      reads); a real caller relies on this, so the dispatcher - not
;      BIOS_GETCH itself - is the right place to add it. ----
FN_CONIN call BIOS_GETCH
	push af
	call BIOS_PUTCH
	pop af
	ret

; ---- Function 2: Console Output - the character to print arrives in E
;      (real CP/M's own documented convention for this function - not A,
;      not C). ----
FN_CONOUT ld a,e
	jp BIOS_PUTCH		; tail call - BIOS_PUTCH's own ret returns to our caller

; ---- Function 9: Print String - DE points at a string terminated by '$'
;      (not null) - real CP/M's own documented convention. Prints up to but
;      not including the '$'. ----
FN_PRINT_STRING ld a,(de)
	cp '$'
	ret z
	call BIOS_PUTCH
	inc de
	jp FN_PRINT_STRING

; ---- Function 11: Get Console Status - returns A=$FF if a keystroke is
;      waiting, A=$00 otherwise. VideoController's own keyboard-status port
;      - the same one BIOS_GETCH itself polls - already carries exactly
;      this bit. ----
FN_CONST ld bc,VIDEO_KBD_STAT
	in a,(c)
	and DATA_AVAILABLE
	ret z
	ld a,$ff
	ret

; ---- Function 12: Get Version Number - returns HL=$0022 (real CP/M's own
;      BCD-style encoding for version 2.2; H=$00 marks this as a plain
;      CP/M system, not MP/M). A fixed constant, not computed from
;      anywhere - real CP/M's own version number is baked into its BDOS
;      the same way. ----
FN_VERSION ld hl,$0022
	ret

; ---- Function 14: Select Disk - E = drive number (0=A, 1=B, ...). Maps
;      directly onto BIOS_SELECT. ----
FN_SELDSK ld a,e
	jp BIOS_SELECT		; tail call - BIOS_SELECT's own ret returns to our caller

; ---- Decodes a real 36-byte FCB's name (DE-pointed, bytes 1-8) and
;      extension (bytes 9-11) into a null-terminated "NAME.EXT" string at
;      FCB_NAME_SCRATCH - trailing spaces in either fixed-width field are
;      dropped, same convention CpmImageReader.decodeFileName() already
;      uses for real CP/M disks. Always consumes exactly the fixed 8+3
;      bytes regardless of where a name/extension's own real content
;      ends (real CP/M names are always left-justified, space-padded, so
;      once a space is seen the rest of that field is guaranteed to be
;      spaces too) - simpler to get right than early-exiting a loop and
;      separately skipping whatever's left. Leaves DE unchanged. ----
DECODE_FCB_NAME push de
	inc de			; de -> byte 1 (name start) - skip DR (byte 0)
	ld hl,FCB_NAME_SCRATCH
	ld b,8
DFN_NAME_LOOP ld a,(de)
	inc de
	cp ' '
	jp z,DFN_NAME_SKIP
	ld (hl),a
	inc hl
DFN_NAME_SKIP dec b
	jp nz,DFN_NAME_LOOP
	; de now points at byte 9 (extension start) - always, regardless of
	; how many real characters the name itself had.
	ld a,(de)
	cp ' '
	jp z,DFN_DONE		; blank extension - stop here, no dot
	ld (hl),'.'
	inc hl
	ld b,3
DFN_EXT_LOOP ld a,(de)
	inc de
	cp ' '
	jp z,DFN_EXT_SKIP
	ld (hl),a
	inc hl
DFN_EXT_SKIP dec b
	jp nz,DFN_EXT_LOOP
DFN_DONE ld (hl),0		; null terminator
	pop de
	ret

; ---- Function 15: Open File - DE points at a real 36-byte FCB (bytes
;      1-8 name, 9-11 extension, space-padded - real CP/M's own fixed 8.3
;      layout, byte-for-byte the same convention this project's own
;      CpmImageReader already uses for real CP/M disks). Decodes it into
;      a null-terminated "NAME.EXT" string this project's own
;      BDOS_FIND_FILE already expects (staged via TRANSFER, not a
;      register - see TRANSFER's own comment), then translates
;      BDOS_FIND_FILE's 1/0 (found/not found) return into real CP/M's own
;      0/$FF (found/not found) convention for this function - the
;      opposite polarity, a real detail of the real spec, not an
;      inconsistency in this adapter. Also zeroes the FCB's own CR
;      (current record, byte 32) field on success, matching a
;      freshly-opened file's real starting state. ----
FN_OPEN push de		; keep the FCB pointer - needed again below
	call DECODE_FCB_NAME
	ld hl,FCB_NAME_SCRATCH
	ld (TRANSFER),hl
	call BDOS_FIND_FILE
	pop de
	or a
	jp z,FN_OPEN_NOTFOUND
	ld hl,32
	add hl,de
	ld (hl),0		; CR = 0
	xor a
	ret
FN_OPEN_NOTFOUND ld a,$ff
	ret

; ---- Function 20: Read Sequential - DE points at the FCB opened by the
;      last Open File call. Stages the current DMA address (set by
;      function 26, defaulting to $0080 - real CP/M's own default) via
;      TRANSFER, then calls BDOS_READ_RECORD - which tracks "which file,
;      how far into it" via its OWN internal state (bdos.c's own static
;      read cursor), not via this FCB's own S1/S2/CR fields at all - see
;      this file's own header comment on the single-open-file
;      limitation that follows from that. Translates BDOS_READ_RECORD's
;      1/0 (read/done) return into real CP/M's own 0/1 (success/EOF)
;      convention - also the opposite polarity from Open File's own
;      0/$FF, a real, easy-to-get-backward detail of the real CP/M spec
;      itself. Also advances the FCB's own CR field (byte 32, wrapping at
;      128) on success, for display/compatibility - a real multi-extent
;      file's own EX/S2 advancement is left entirely to bdos.c's own
;      internal state, not derived from this FCB's fields. ----
FN_READ_SEQ push de
	ld hl,(DMA_ADDRESS)
	ld (TRANSFER),hl
	call BDOS_READ_RECORD
	pop de
	or a
	jp z,FN_READ_SEQ_EOF
	ld hl,32
	add hl,de
	ld a,(hl)
	inc a
	and $7f			; wrap 128 -> 0
	ld (hl),a
	xor a
	ret
FN_READ_SEQ_EOF ld a,1
	ret

; ---- Function 25: Get Current Disk - returns the currently selected
;      drive (0=A, 1=B, ...) in A. FloppyController's own Drive Select
;      register is already readable, so no separate local tracking is
;      needed here at all - this just reads it straight back. ----
FN_CURDSK ld bc,FD_DRIVE
	in a,(c)
	ret

; ---- Function 26: Set DMA Address - DE = the address subsequent Read/
;      Write Sequential calls transfer their 128-byte record to/from.
;      Defaults to $0080 (DMA_ADDRESS's own initial value below) if never
;      called - real CP/M's own default DMA address. ----
FN_SETDMA ld (DMA_ADDRESS),de
	ret

DMA_ADDRESS defw $0080
FCB_NAME_SCRATCH defb 0,0,0,0,0,0,0,0,0,0,0,0,0
