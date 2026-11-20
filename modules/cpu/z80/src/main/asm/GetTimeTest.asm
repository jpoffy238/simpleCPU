; Proves BIOS.asm's new BIOS_GET_TIME primitive works end to end through a
; real BIOS call, the same role DiskTest.asm/GraphicsTest.asm already play
; for the disk/graphics primitives.
;
; Calls BIOS_GET_TIME once with HL pointing at TIME, then loops forever -
; Test_Z80GetTime reads the 4 bytes back from TIME and reassembles them as
; a little-endian unsigned long to compare against a wall-clock bracket.

BIOS_GET_TIME equ $F021
TIME equ $2000

	org $1000

START	ld hl,TIME
	call BIOS_GET_TIME
DONE	jp DONE
