; Regression test for real hardware scrolling (BIOS.asm's PC_SCROLL) -
; same scenario as the 6502 side's own
; printingPastTheBottomScrollsRepeatedlyWithoutFaulting: prints far more
; lines than one V_ROWS=24 screen holds, forcing several scrolls - enough
; to wrap the 2048-byte ring at least once (60 rows * 80 cols = 4800 >
; 2048*2) - then prints "Z". The trailing Z only appears if the whole
; loop ran to completion without faulting on an unmapped address (the
; same class of bug the 6502 side's own PC_LF ring-wrap gap once had).

BIOS_PUTCH equ $F000
BIOS_VIDEO_INIT equ $F006

	org $1000

START	call BIOS_VIDEO_INIT
	ld b,60
LOOP	ld a,'A'
	call BIOS_PUTCH
	ld a,$0A		; LF
	call BIOS_PUTCH
	dec b
	jp nz,LOOP
	ld a,'Z'
	call BIOS_PUTCH

DONE	jp DONE
