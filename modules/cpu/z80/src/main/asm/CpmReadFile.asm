; Phase 2 test fixture for the real-CP/M-compatible BDOS dispatcher
; (CpmCompat.asm) - builds a real 36-byte FCB for "PAGE.COM" by hand, then
; uses the GENUINE CP/M 2.2 calling convention (CALL 5) to select drive 0,
; open the file, and read it record by record into a fixed scratch buffer
; ($2000) - proving Open File/Read Sequential's delegation into the
; existing BDOS_FIND_FILE/BDOS_READ_RECORD primitives against a real disk
; image, through the real FCB-based convention, not this project's own
; custom plain-filename one.

	org $0100

START	ld e,0
	ld c,14			; Select Disk - drive 0
	call $0005

	ld de,FCB
	ld c,15			; Open File
	call $0005
	or a
	jp nz,DONE		; A != 0 means not found - stop here, nothing to read

	ld hl,$2000		; scratch buffer - the Java test reads this back
READ_LOOP ld d,h
	ld e,l
	push hl			; our own running scratch pointer - save it, the
				; next two calls will both clobber hl
	ld c,26			; Set DMA - de = current scratch address
	call $0005
	ld de,FCB
	ld c,20			; Read Sequential
	call $0005
	pop hl
	or a
	jp nz,DONE		; A != 0 means end of file
	ld de,128
	add hl,de
	jp READ_LOOP

DONE	jp DONE

; A real 36-byte FCB, hand-built with "PAGE    COM" (8+3, space-padded) -
; the exact real CP/M fixed-width convention CpmCompat.asm's own
; DECODE_FCB_NAME expects, not a convenience this project invented.
FCB	defb 0			; DR
	defm "PAGE    "	; F1-F8
	defm "COM"		; T1-T3
	defb 0,0,0,0		; EX,S1,S2,RC
	defb 0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0	; AL0-AL15
	defb 0			; CR
	defb 0,0,0		; R0-R2
