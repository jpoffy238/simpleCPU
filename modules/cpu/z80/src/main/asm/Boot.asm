; The real Z80 cold-boot entry point. Z80Main loads BIOS.z80 and
; bdos.ihx onto the bus before jumping here - same order a real CP/M
; boot ROM would use (BIOS, then BDOS) - and this is the minimal stub a
; real boot ROM would hand off to next: select drive 0, ask BDOS_RUN_FILE
; to find "CCP.COM" and load-and-jump into it. Exactly the mechanism
; Test_Z80BdosRunFile already proves end to end via RunFileTest.asm (this
; file mirrors it - same logic, used for real now rather than just to
; prove the primitive works).
;
; Never returns if CCP.COM is found: BDOS_RUN_FILE loads it to BOOTADDR
; ($1000) and jumps straight in, landing in CCP's own real "A>" prompt -
; CCP's own crt0/main calls bios_video_init() itself, so this stub
; doesn't need to. If CCP.COM is missing (e.g. drive 0's image hasn't had
; it written yet - see disk-put.sh), prints NOTFOUND and halts rather
; than silently hanging with no explanation.

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
