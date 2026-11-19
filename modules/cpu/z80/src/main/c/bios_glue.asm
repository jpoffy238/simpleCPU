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
; SDCC's own default z80 calling convention (sdcccall(1)): a single 8-bit
; argument/return value goes through A, a pointer or 16-bit value through
; HL (confirmed by compiling probes and reading the emitted asm directly,
; not just trusting docs) - an exact match for BIOS.asm's own "A in/out,
; BC/DE/HL preserved" convention (see BIOS.asm's header). bios.h's
; "stage one value, then trigger" calls (built for cc65's one-C-parameter
; __fastcall__ limit, not any real Z80 limitation - BIOS.asm's own disk
; primitives take their second/third value directly in E/HL when called
; from hand-written Z80 code) need somewhere to stage that second value
; between the bios_set_x()/bios_set_buffer() call and the real
; bios_read_sector() etc. call that follows.
;
; ADDRLO/ADDRHI are NOT a linker-placed symbol (unlike a plain local
; label) - they're a fixed, bare RAM address, because bdos.c's own
; TRANSFER macro (modules/cpu/c-shared/bdos.c) reads/writes this exact
; same address directly, by raw pointer cast, completely independent of
; whatever this file links to. $0040/$0041 sits safely between SDCC's own
; fixed reset-vector/RST-trampoline table ($0000-$003F) and its generic
; crt0 entry stub ($0100-$010F) - never used by anything else. staged_x
; has no such cross-file contract (nothing outside this file reads it by
; raw address), so $0042 is just a locally convenient choice, not a
; negotiated one.
;
; SDCC prefixes C-level identifiers with "_" when emitting/linking -
; bios_putch (C) <-> _bios_putch (here), same convention cc65 uses.

.module bios_glue

BIOS_PUTCH        = 0xF000
BIOS_GETCH        = 0xF003
BIOS_VIDEO_INIT   = 0xF006
BIOS_GOTOXY       = 0xF009
BIOS_SELECT       = 0xF00C
BIOS_READ_SECTOR  = 0xF00F
BIOS_WRITE_SECTOR = 0xF012
BIOS_FORMAT_TRACK = 0xF015
BIOS_RESTORE      = 0xF018

ADDRLO   = 0x0040
ADDRHI   = 0x0041
STAGED_X = 0x0042

.area _CODE

.globl _bios_putch
.globl _bios_getch
.globl _bios_video_init
.globl _bios_gotoxy
.globl _bios_select
.globl _bios_set_buffer
.globl _bios_set_x
.globl _bios_read_sector
.globl _bios_write_sector
.globl _bios_format_track
.globl _bios_restore

; void bios_putch(char c) - c arrives in A
_bios_putch::
	jp	BIOS_PUTCH

; char bios_getch(void) - return value goes out in A
_bios_getch::
	jp	BIOS_GETCH

; void bios_video_init(void) - Z80-only, see bios.h
_bios_video_init::
	jp	BIOS_VIDEO_INIT

; void bios_gotoxy(unsigned char row) - row arrives in A, col staged via
; bios_set_x() first (same pairing bios.h already documents)
_bios_gotoxy::
	ld	hl, #STAGED_X
	ld	e, (hl)
	jp	BIOS_GOTOXY

; void bios_select(unsigned char drive) - drive arrives in A
_bios_select::
	jp	BIOS_SELECT

; void bios_set_buffer(void *ptr) - ptr arrives in HL
_bios_set_buffer::
	ld	(ADDRLO), hl
	ret

; void bios_set_x(unsigned char value) - value arrives in A
_bios_set_x::
	ld	(STAGED_X), a
	ret

; void bios_read_sector(unsigned char track) - track arrives in A (left
; untouched below); sector staged via bios_set_x(), buffer via
; bios_set_buffer()
_bios_read_sector::
	ld	hl, #STAGED_X
	ld	e, (hl)
	ld	hl, (ADDRLO)
	jp	BIOS_READ_SECTOR

; void bios_write_sector(unsigned char track) - same staging as
; bios_read_sector() above
_bios_write_sector::
	ld	hl, #STAGED_X
	ld	e, (hl)
	ld	hl, (ADDRLO)
	jp	BIOS_WRITE_SECTOR

; void bios_format_track(unsigned char track) - fill byte staged via
; bios_set_x()
_bios_format_track::
	ld	hl, #STAGED_X
	ld	e, (hl)
	jp	BIOS_FORMAT_TRACK

; void bios_restore(void) - no parameters
_bios_restore::
	jp	BIOS_RESTORE
