; ca65 glue bridging C to BDOS's fixed-address jump table (see bdos.h for
; the C-facing API and bdos_jumptable.s for the ROM-side table these
; addresses point into). Simpler than bios_glue.s - every BDOS primitive
; takes its input (if any) via ADDRLO/ADDRHI, already staged by the caller's
; own bios_set_buffer() call, and returns its result (if any) directly in A,
; same as cc65's normal single-byte return convention - so there's no
; parameter to stage here at all, just a direct call-through.
;
; Link this alongside bios_glue.s into any program that wants to call BDOS
; (ccp.c, say) - BDOS.bin itself is a separate cl65 build and never links
; against this file.

BDOS_REWIND_DIR  = $A000
BDOS_NEXT_ENTRY  = $A003
BDOS_FIND_FILE   = $A006
BDOS_READ_RECORD = $A009
BDOS_DELETE_FILE = $A00C
BDOS_CREATE_FILE = $A00F
BDOS_WRITE_RECORD = $A012
BDOS_ENTRY_SIZE   = $A015
BDOS_RENAME_FILE  = $A018

.export _bdos_rewind_dir, _bdos_next_entry, _bdos_find_file, _bdos_read_record
.export _bdos_delete_file, _bdos_create_file, _bdos_write_record
.export _bdos_entry_size, _bdos_rename_file

.segment "CODE"

; void bdos_rewind_dir(void)
_bdos_rewind_dir:
	jmp BDOS_REWIND_DIR	; tail call - its own RTS returns to our caller

; unsigned char bdos_next_entry(void) - return value comes back in A
_bdos_next_entry:
	jmp BDOS_NEXT_ENTRY

; unsigned char bdos_find_file(void) - return value comes back in A
_bdos_find_file:
	jmp BDOS_FIND_FILE

; unsigned char bdos_read_record(void) - return value comes back in A
_bdos_read_record:
	jmp BDOS_READ_RECORD

; unsigned char bdos_delete_file(void) - return value comes back in A
_bdos_delete_file:
	jmp BDOS_DELETE_FILE

; unsigned char bdos_create_file(void) - return value comes back in A
_bdos_create_file:
	jmp BDOS_CREATE_FILE

; unsigned char bdos_write_record(void) - return value comes back in A
_bdos_write_record:
	jmp BDOS_WRITE_RECORD

; unsigned int bdos_entry_size(void) - return value comes back in A (low)/X (high)
_bdos_entry_size:
	jmp BDOS_ENTRY_SIZE

; unsigned char bdos_rename_file(void) - return value comes back in A
_bdos_rename_file:
	jmp BDOS_RENAME_FILE
