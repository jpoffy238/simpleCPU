; Phase 1 test fixture for the new real-CP/M-compatible BDOS dispatcher
; (CpmCompat.asm) - a tiny program using the GENUINE CP/M 2.2 calling
; convention (CALL 5, function number in C), not this project's own custom
; BIOS_*/BDOS_* ABI at all. Prints "HELLO" via a real BDOS function 9
; (Print String) call, then loops forever.
;
; Loads at $0100 - real CP/M's own fixed TPA (Transient Program Area)
; entry point, not a convention this project invented. Proves the
; dispatcher's function 9 wiring (and its own delegation into the existing
; BIOS_PUTCH primitive) purely through the real ABI, with no knowledge of
; this project's own BIOS_PUTCH address at all - exactly the real-world
; property this whole feature exists to prove.

	org $0100

START	ld de,MESSAGE
	ld c,9
	call $0005
DONE	jp DONE

MESSAGE defm "HELLO"
	defb '$'
