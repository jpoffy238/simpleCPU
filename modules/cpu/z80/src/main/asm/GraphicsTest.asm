; Proves BIOS.asm's new graphics primitives (BIOS_GRAPHICS_MODE/
; BIOS_SET_PIXEL) work end to end through real BIOS calls - same role
; DiskTest.asm already plays for the disk primitives. Exercises: (0,0) and
; (7,0), the two ends of the same byte's bit range (mod-8 boundary);
; (319,199), the bottom-right corner of the 320x200 bitmap (column needs
; the full 16-bit HL, since 319 > 255); and (3,2) set then cleared, to
; prove the AND-mask path actually clears a bit rather than just never
; setting it - the same four cases Test_BIOS's own graphics test covers on
; the 6502 side.
;
; Nothing is printed here - Test_Z80GraphicsPrimitives checks
; VideoController.getPixel()/isGraphicsMode() directly, the same way
; Test_BIOS's own graphics test does, rather than needing BIOS_PUTCH to
; report a result.

BIOS_GRAPHICS_MODE equ $F01B
BIOS_SET_PIXEL      equ $F01E
MODE_GRAPHICS equ 1

	org $1000

START	ld a,MODE_GRAPHICS
	call BIOS_GRAPHICS_MODE

	ld a,0			; row 0
	ld e,1			; on
	ld hl,0			; col 0
	call BIOS_SET_PIXEL	; -> (0,0) on

	ld a,0
	ld e,1
	ld hl,7
	call BIOS_SET_PIXEL	; -> (7,0) on

	ld a,199
	ld e,1
	ld hl,319
	call BIOS_SET_PIXEL	; -> (319,199) on

	ld a,2
	ld e,1
	ld hl,3
	call BIOS_SET_PIXEL	; -> (3,2) on

	ld a,2
	ld e,0			; off
	ld hl,3
	call BIOS_SET_PIXEL	; -> (3,2) off

DONE	jp DONE
