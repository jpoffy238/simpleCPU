; sdasz80 glue bridging C to BIOS.z80's fixed-address jump table primitives.
; See ../../../c-shared/bios.h for the C-facing API, shared with the 6502
; side's own bios_glue.s - this file is the Z80/SDCC-only half of that split
; (incompatible assembler syntax, identical idea: JP straight to a fixed
; address).
;
; This is a separate toolchain (SDCC/sdasz80/sdldz80) from z80-asm, which
; assembles BIOS.asm/Demo.asm - no shared symbol table between them either,
; so the jump table addresses are duplicated here too, same as Demo.asm
; already duplicates them against BIOS.asm. Keep both in sync if anything
; moves.
;
; SDCC's own default z80 calling convention (sdcccall(1)) passes a single
; byte argument in A and returns a single byte result in A - an exact match
; for BIOS.asm's own "A in/out, BC/DE/HL preserved" convention (see
; BIOS.asm's header), so every stub below is just a bare JP, no register
; shuffling needed.
;
; SDCC prefixes C-level identifiers with "_" when emitting/linking -
; bios_putch (C) <-> _bios_putch (here), same convention cc65 uses.

.module bios_glue

BIOS_PUTCH       = 0xF000
BIOS_GETCH       = 0xF003
BIOS_VIDEO_INIT  = 0xF006
; BIOS_GOTOXY ($F009) deliberately has no stub here yet - it takes row in A,
; col in E directly, a different convention from bios.h's "row in A, col
; staged via bios_set_x() first" (built for cc65's one-parameter
; __fastcall__ limit, see bios.h). No Z80 C program calls it yet; wire it up
; (staging col from the glue-owned byte bios_set_x() writes into E) once one
; does, rather than ship a stub that quietly ignores col until then.

.area _CODE

.globl _bios_putch
.globl _bios_getch
.globl _bios_video_init

_bios_putch::
	jp	BIOS_PUTCH

_bios_getch::
	jp	BIOS_GETCH

_bios_video_init::
	jp	BIOS_VIDEO_INIT
