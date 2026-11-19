; Minimal crt0 for a Z80 C program meant to be loaded from disk and run
; via BDOS_RUN_FILE (bdos.c's load_and_run(), see its own comment) -
; entered by a plain CALL from BDOS's own already-running C code
; (`((void (*)(void)) BOOTADDR)()`, a real `call 0x1000` under SDCC -
; confirmed by compiling that exact idiom and reading the emitted asm
; directly), not by a Z80 power-on reset the way SDCC's own default crt0
; (see Makefile's own hello.ihx/ccp.ihx-when-booted-directly rule) is
; built for.
;
; SP is NOT reset here, unlike SDCC's own default crt0's own
; "LD SP,0000": SP is already whatever BDOS's own call chain left it as,
; which is already valid (BDOS itself is C running on the same real
; hardware stack) - there is no power-on "SP is garbage" problem to
; solve. Resetting SP here would actually BREAK things: the final RET
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
; gsinit IS called, though, after a real two-part bug found 2026-11-19
; porting edit.c. First part: SDCC's --no-std-crt0 strips the CALLER of
; "zero every global's BSS, then copy every non-zero-initialized global
; from _INITIALIZER to _INITIALIZED" - but not that work itself, which
; has to happen somewhere or every such global starts out holding
; whatever garbage was already in RAM. ccp.c's only global
; (`current_drive = 0`) happened to want zero anyway, so this went
; unnoticed on fresh RAM; edit.c's `line_count = 1`/`total_pages = 1`/
; `dirty = DIRTY_FULL` are not zero, and the editor's own draw logic
; silently rendered nothing at all as a result (insert_char() itself
; worked correctly throughout - confirmed by a step-by-step diagnostic
; counting real BIOS_GETCH calls and their returned bytes - the bug was
; specifically in this initialization never running, not in the BIOS
; layer).
;
; Second part, found immediately after the first fix didn't work: the
; actual zero/copy LOOP is not something every compiled file
; contributes a share of into a shared "gsinit" area, the way the jump
; table pattern (bdos_jumptable.asm, linked first) works elsewhere in
; this project - it's fixed code that ONLY SDCC's own crt0.s provides
; (confirmed by reading share/sdcc/lib/src/z80/crt0.s directly), written
; once against linker-computed aggregate symbols (l__DATA/s__DATA,
; l__INITIALIZER/s__INITIALIZED/s__INITIALIZER - total size and start
; address across every linked file's own share of each area, not
; per-file values). A first attempt here that just supplied an empty
; "gsinit::" label and a bare "ret" compiled and linked without error,
; and still did nothing - confirmed afterward by checking the real
; linked edit.map, which showed _GSINIT itself sized at 0 bytes. The
; loop below is that same crt0.s logic, verbatim.
;
; No __sdcc_external_startup hook (SDCC's own crt0 calls that first, and
; only runs the loop below if it returns 0) - nothing in this codebase
; ever defines that symbol, so running it unconditionally is equivalent
; and simpler.
;
; Link this FIRST on the command line, ahead of the program's own .rel
; (see the Makefile) - sdldz80 places each file's _CODE content in the
; order its object file appears, so this file's own code lands at
; exactly --code-loc, matching BOOTADDR (bdos.c, #ifdef __SDCC).

.module cpm_crt0

.globl _main
.globl l__DATA
.globl s__DATA
.globl l__INITIALIZER
.globl s__INITIALIZED
.globl s__INITIALIZER

.area _CODE

_entry::
	call	gsinit
	call	_main
	ret

	.area _GSINIT
gsinit::
	; Zero every global's BSS.
	ld	bc, #l__DATA
	ld	a, b
	or	a, c
	jr	Z, zeroed_data
	ld	hl, #s__DATA
	ld	(hl), #0x00
	dec	bc
	ld	a, b
	or	a, c
	jr	Z, zeroed_data
	ld	e, l
	ld	d, h
	inc	de
	ldir
zeroed_data:

	; Copy every explicitly, non-zero-initialized global from
	; _INITIALIZER (the compiler-embedded literal values) to
	; _INITIALIZED (where the variable actually lives).
	ld	bc, #l__INITIALIZER
	ld	a, b
	or	a, c
	jr	Z, gsinit_next
	ld	de, #s__INITIALIZED
	ld	hl, #s__INITIALIZER
	ldir
gsinit_next:

	.area _GSFINAL
	ret
