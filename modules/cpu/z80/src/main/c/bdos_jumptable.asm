; BDOS ROM jump table - fixed, frozen entry points at the very start of
; BDOS's own image ($A000, matching the 6502 side's own bdos_jumptable.s
; exactly, for easy cross-CPU comparison). Any Z80 caller (hand-written
; assembly directly, or C through bdos_glue.asm's stubs) JPs to these exact
; addresses; moving a function's actual implementation within bdos.c's
; compiled _CODE is always safe as long as its slot here still JPs to the
; right place - same contract as BIOS.asm's own jump table.
;
; Must be linked FIRST on the command line, ahead of bdos.rel itself (see
; the Makefile) - sdldz80 places each file's _CODE content in the order
; its object file appears on the link command, so this file's own JPs land
; at exactly --code-loc ($A000), with bdos.c's real function bodies packed
; in right after.
;
; SDCC prefixes C-level identifiers with "_" when emitting/linking -
; bdos_next_entry (C) <-> _bdos_next_entry (here), same convention cc65
; uses.

.module bdos_jumptable

.globl _bdos_rewind_dir
.globl _bdos_next_entry
.globl _bdos_find_file
.globl _bdos_read_record
.globl _bdos_delete_file
.globl _bdos_create_file
.globl _bdos_write_record
.globl _bdos_entry_size
.globl _bdos_rename_file
.globl _bdos_run_file
.globl _bdos_free_blocks

.area _CODE

	jp	_bdos_rewind_dir	; $A000 BDOS_REWIND_DIR
	jp	_bdos_next_entry	; $A003 BDOS_NEXT_ENTRY
	jp	_bdos_find_file		; $A006 BDOS_FIND_FILE
	jp	_bdos_read_record	; $A009 BDOS_READ_RECORD
	jp	_bdos_delete_file	; $A00C BDOS_DELETE_FILE
	jp	_bdos_create_file	; $A00F BDOS_CREATE_FILE
	jp	_bdos_write_record	; $A012 BDOS_WRITE_RECORD
	jp	_bdos_entry_size	; $A015 BDOS_ENTRY_SIZE
	jp	_bdos_rename_file	; $A018 BDOS_RENAME_FILE
	jp	_bdos_run_file		; $A01B BDOS_RUN_FILE
	jp	_bdos_free_blocks	; $A01E BDOS_FREE_BLOCKS
