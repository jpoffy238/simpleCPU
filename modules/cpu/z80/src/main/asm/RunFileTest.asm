; Proves BDOS_RUN_FILE (bdos.c's load_and_run(), the real CP/M "load a
; named file record by record to BOOTADDR and jump into it" mechanism)
; works end to end on Z80 - the one deliberately-deferred BDOS primitive
; from the original bdos/ccp port, now wired up. Selects drive 0, stages
; "CCP.COM" as the filename to run (same ADDRLO staging convention
; bios_set_buffer()/bdos_run_file() already use - see bios_glue.asm),
; then calls BDOS_RUN_FILE directly.
;
; Never expected to return: if CCP.COM is found (it will be - the test
; seeds the disk with the real ccp.ihx's own bytes, built with
; cpm_crt0.asm so it's genuinely loadable this way, see the Makefile's
; own comment), BDOS_RUN_FILE loads it to BOOTADDR ($1000) and jumps
; straight in, landing in CCP's own real prompt loop - Test_Z80BdosRunFile
; checks for CCP's own "A>" prompt on screen, not anything this file
; prints. The NOTFOUND path below only runs if something's wrong with the
; disk setup - a diagnostic, not the expected outcome.

BIOS_PUTCH equ $F000
BIOS_SELECT equ $F00C
BDOS_RUN_FILE equ $A01B
ADDRLO equ $0040

	org $1000

START	xor a
	call BIOS_SELECT	; drive 0
	ld hl,FILENAME
	ld (ADDRLO),hl
	call BDOS_RUN_FILE	; never returns if CCP.COM was found

	ld hl,NOTFOUND_MSG
PRINT_LOOP ld a,(hl)
	or 0
	jp z,DONE
	call BIOS_PUTCH
	inc hl
	jp PRINT_LOOP

DONE	jp DONE

FILENAME defm "CCP.COM"
	defb 0
NOTFOUND_MSG defm "NOTFOUND"
	defb 0
