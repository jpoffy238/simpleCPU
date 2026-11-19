; Minimal crt0 for a Z80 C program meant to be loaded from disk and run
; via BDOS_RUN_FILE (bdos.c's load_and_run(), see its own comment) -
; entered by a plain CALL from BDOS's own already-running C code
; (`((void (*)(void)) BOOTADDR)()`, a real `call 0x1000` under SDCC -
; confirmed by compiling that exact idiom and reading the emitted asm
; directly), not by a Z80 power-on reset the way SDCC's own default crt0
; (see Makefile's own hello.ihx/ccp.ihx-when-booted-directly rule) is
; built for.
;
; This is why it can be this short: SP is already whatever BDOS's own
; call chain left it as, which is already valid (BDOS itself is C running
; on the same real hardware stack) - there is no power-on "SP is garbage"
; problem to solve the way SDCC's own default crt0's "LD SP,0000" exists
; for. Resetting SP here would actually BREAK things: the final RET
; below has to pop the SAME return address `call 0x1000` pushed, so SP
; must still be exactly where that CALL left it once CALL _main's own
; matching RET unwinds - confirmed by hand (compiled a trivial two-file
; program with this exact stub, read the linked .ihx back byte for byte,
; and traced it: `call _main` / `ret`, nothing else, 10 bytes total,
; landing at exactly --code-loc with no separate reset-vector/RST-table/
; $0100-stub regions at all - unlike a normal app, which always carries
; those, --no-std-crt0 strips them, same reason BDOS.ihx's own build
; already needs it, see bdos_jumptable.asm).
;
; No BSS-zeroing (SDCC's own default crt0's GSINIT work) either - a real
; simplification for small test/demo programs with no initialized/static
; data to worry about, not something safe to assume for every future
; loadable program; revisit if one ever needs it.
;
; Link this FIRST on the command line, ahead of the program's own .rel
; (see the Makefile) - sdldz80 places each file's _CODE content in the
; order its object file appears, so this file's own code lands at
; exactly --code-loc, matching BOOTADDR (bdos.c, #ifdef __SDCC).

.module cpm_crt0

.globl _main

.area _CODE

_entry::
	call	_main
	ret
