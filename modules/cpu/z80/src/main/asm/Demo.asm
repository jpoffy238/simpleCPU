; First real program to boot on Z80VideoDiskMachine - proves the whole
; pipeline (CPU -> port bus -> VideoController -> screen, and the
; keyboard path back) actually works end to end. Calls into BIOS.asm's
; fixed jump table addresses (duplicated here as equ constants, same
; frozen-ABI convention the 6502 side's MONITOR.asm uses against its own
; BIOS.asm) rather than assuming anything about BIOS's own internals.

BIOS_PUTCH equ $F000
BIOS_GETCH equ $F003
BIOS_VIDEO_INIT equ $F006
BIOS_GOTOXY equ $F009

	org $1000

START	call BIOS_VIDEO_INIT
	ld hl,GREETING
PRINT_LOOP ld a,(hl)
	or 0
	jp z,ECHO_LOOP
	call BIOS_PUTCH
	inc hl
	jp PRINT_LOOP

; ---- echoes every keystroke back to the screen, forever - about the
;      simplest possible proof that both halves of the console (output
;      via BIOS_PUTCH, input via BIOS_GETCH) are really working. ----
ECHO_LOOP call BIOS_GETCH
	call BIOS_PUTCH
	jp ECHO_LOOP

GREETING defm "HELLO FROM THE Z80"
	 defb $0D,$0A,0
