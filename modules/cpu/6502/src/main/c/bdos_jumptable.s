; BDOS ROM jump table - fixed, frozen entry points at the very start of
; BDOS's ROM ($A000, see bdos.cfg), mirroring BIOS.asm's own jump table.
; MONITOR.asm (hand assembly) JSRs to these addresses directly; any
; cc65-compiled caller goes through bdos_glue.s's stubs instead, which JSR
; to the exact same addresses. Moving a function's actual implementation
; within bdos.c's CODE segment is always safe as long as its slot here
; still JMPs to the right place - same contract as BIOS.asm's table.
;
; cc65 prefixes C-level identifiers with "_" when emitting/linking against
; assembly - bdos_next_entry (C) <-> _bdos_next_entry (here).

.import _bdos_rewind_dir
.import _bdos_next_entry
.import _bdos_find_file
.import _bdos_read_record
.import _bdos_delete_file
.import _bdos_create_file
.import _bdos_write_record
.import _bdos_entry_size
.import _bdos_rename_file

.segment "JUMPTABLE"
	jmp _bdos_rewind_dir	; $A000 BDOS_REWIND_DIR
	jmp _bdos_next_entry	; $A003 BDOS_NEXT_ENTRY
	jmp _bdos_find_file	; $A006 BDOS_FIND_FILE
	jmp _bdos_read_record	; $A009 BDOS_READ_RECORD
	jmp _bdos_delete_file	; $A00C BDOS_DELETE_FILE
	jmp _bdos_create_file	; $A00F BDOS_CREATE_FILE
	jmp _bdos_write_record	; $A012 BDOS_WRITE_RECORD
	jmp _bdos_entry_size	; $A015 BDOS_ENTRY_SIZE
	jmp _bdos_rename_file	; $A018 BDOS_RENAME_FILE
