; Proves BIOS.asm's new disk primitives (BIOS_SELECT/BIOS_READ_SECTOR/
; BIOS_WRITE_SECTOR) actually work end to end through real BIOS calls,
; the same role Demo.asm already plays for the console primitives - before
; BDOS gets built on top of them, so a bug here isn't mistaken for a BDOS
; bug later.
;
; Writes a known pattern to track 0, sector 1, clears the in-memory buffer,
; reads the same sector back, and prints "OK" if every byte round-tripped
; correctly or "FAIL" (plus the first mismatching byte's offset, decimal)
; if not.

BIOS_PUTCH        equ $F000
BIOS_VIDEO_INIT   equ $F006
BIOS_SELECT       equ $F00C
BIOS_READ_SECTOR  equ $F00F
BIOS_WRITE_SECTOR equ $F012

	org $1000

START	call BIOS_VIDEO_INIT
	xor a
	call BIOS_SELECT	; drive 0

	ld hl,BUFFER
	ld b,128
	ld c,0
FILL_LOOP ld (hl),c
	inc hl
	inc c
	djnz FILL_LOOP

	xor a			; track 0
	ld e,1			; sector 1
	ld hl,BUFFER
	call BIOS_WRITE_SECTOR

	ld hl,BUFFER
	ld b,128
CLEAR_LOOP ld (hl),0
	inc hl
	djnz CLEAR_LOOP

	xor a			; track 0
	ld e,1			; sector 1
	ld hl,BUFFER
	call BIOS_READ_SECTOR

	ld hl,BUFFER
	ld b,128
	ld c,0
VERIFY_LOOP ld a,(hl)
	cp c
	jp nz,MISMATCH
	inc hl
	inc c
	djnz VERIFY_LOOP

	ld hl,OK_MSG
	jp PRINT_LOOP

MISMATCH ld hl,FAIL_MSG
PRINT_LOOP ld a,(hl)
	or 0
	jp z,DONE
	call BIOS_PUTCH
	inc hl
	jp PRINT_LOOP

DONE	jp DONE

OK_MSG   defm "OK"
	 defb 0
FAIL_MSG defm "FAIL"
	 defb 0

BUFFER	defb 0
