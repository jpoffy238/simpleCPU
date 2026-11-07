; ca65 glue bridging C to BIOS.asm's fixed-address jump table primitives.
; See bios.h for the C-facing API and why multi-parameter primitives are
; split into "stage one value, then trigger" calls instead of one function
; taking every parameter at once.
;
; This is a separate toolchain (ca65/cl65/cc65) from crasm, which assembles
; BIOS.asm/MONITOR.asm - no shared symbol table between them either, so the
; jump table addresses are duplicated here too, same as MONITOR.asm already
; duplicates them against BIOS.asm. Keep all three in sync if anything moves.
;
; cc65 prefixes C-level identifiers with "_" when emitting/linking against
; assembly - bios_putch (C) <-> _bios_putch (here).

BIOS_PUTCH        = $F000
BIOS_GETCH        = $F003
BIOS_SELECT       = $F006
BIOS_READ_SECTOR  = $F009
BIOS_WRITE_SECTOR = $F00C
BIOS_FORMAT_TRACK = $F00F
BIOS_RESTORE      = $F012
BIOS_GOTOXY       = $F018

; zero page: shared ABI with BIOS.asm/MONITOR.asm - the sector-buffer address
; parameter for BIOS_READ_SECTOR/BIOS_WRITE_SECTOR.
ADDRLO = $01
ADDRHI = $02

.export _bios_putch, _bios_getch, _bios_select
.export _bios_set_buffer, _bios_set_x
.export _bios_read_sector, _bios_write_sector, _bios_format_track
.export _bios_restore
.export _bios_gotoxy

; Glue-owned staging byte for the second hardware parameter - not part of the
; BIOS.asm/MONITOR.asm ABI, just this glue layer's own scratch. Lives wherever
; the linked .cfg places the ZEROPAGE segment (see simplecpu.cfg) - not a
; fixed address the way ADDRLO/ADDRHI above have to be.
.segment "ZEROPAGE"
staged_x: .res 1

.segment "CODE"

; void __fastcall__ bios_putch(char c) - c arrives in A
_bios_putch:
	jsr BIOS_PUTCH
	rts

; char bios_getch(void) - return value goes out in A
_bios_getch:
	jmp BIOS_GETCH		; tail call - its own RTS returns to our caller

; void __fastcall__ bios_select(unsigned char drive) - drive in A
_bios_select:
	jsr BIOS_SELECT
	rts

; void __fastcall__ bios_set_buffer(void *ptr) - pointer in A (low) / X (high)
_bios_set_buffer:
	sta ADDRLO
	stx ADDRHI
	rts

; void __fastcall__ bios_set_x(unsigned char value) - value in A
_bios_set_x:
	sta staged_x
	rts

; void __fastcall__ bios_read_sector(unsigned char track) - track in A
_bios_read_sector:
	ldx staged_x
	jsr BIOS_READ_SECTOR
	rts

; void __fastcall__ bios_write_sector(unsigned char track) - track in A
_bios_write_sector:
	ldx staged_x
	jsr BIOS_WRITE_SECTOR
	rts

; void __fastcall__ bios_format_track(unsigned char track) - track in A
_bios_format_track:
	ldx staged_x
	jsr BIOS_FORMAT_TRACK
	rts

; void bios_restore(void) - no parameters
_bios_restore:
	jmp BIOS_RESTORE	; tail call

; void __fastcall__ bios_gotoxy(unsigned char row) - row in A, col staged via
; bios_set_x() first (same "A = track, X = sector"-style pairing as
; bios_read_sector() above)
_bios_gotoxy:
	ldx staged_x
	jsr BIOS_GOTOXY
	rts
