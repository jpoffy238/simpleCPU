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
; Phase 1 (this file, for now): console-only functions - 0 (System Reset),
; 1 (Console Input), 2 (Console Output), 9 (Print String), 11 (Get Console
; Status), 12 (Get Version Number). Anything else returns A=$FF (an
; unimplemented-function error) rather than silently doing the wrong thing -
; disk functions (14 Select Disk/15 Open File/20 Read Sequential/25 Get
; Current Disk/26 Set DMA) are a deliberate, separate next phase, not
; forgotten. A plain linear cp/jp-z chain, not a computed jump table - six
; cases, checked once per BDOS call (never once per character the way
; DO_PUTCH is), so there's no reason to prefer a denser but harder-to-verify
; dispatch over code that's easy to trace by hand.
;
; BC/DE/HL are NOT preserved across a call here, unlike this project's own
; BIOS.asm primitives - real CP/M's own BDOS never promised that either, and
; a real program calling real CP/M never assumes it.

BIOS_PUTCH equ $F000
BIOS_GETCH equ $F003

; VideoController's keyboard-status port (see Z80VideoDiskMachine - base
; $00, so this is a bare port number, not an offset).
VIDEO_KBD_STAT equ $03
DATA_AVAILABLE equ $01

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
