; sdasz80 glue bridging C to BDOS's fixed-address jump table (see
; ../../../c-shared/bdos.h for the C-facing API and bdos_jumptable.asm for
; the ROM-side table these addresses point into). Simpler than
; bios_glue.asm - every BDOS primitive takes no parameters at all (input,
; if any, goes through ADDRLO/ADDRHI, already staged by the caller's own
; bios_set_buffer() call) and returns its result (if any) directly in A or
; HL, SDCC's own normal return convention - so there's no staging to do
; here, just a direct jp-through.
;
; Link this alongside bios_glue.asm into any program that wants to call
; BDOS (ccp.c, say) - BDOS.ihx itself is a separate SDCC build (see
; bdos_jumptable.asm/Makefile) and never links against this file.

.module bdos_glue

BDOS_REWIND_DIR   = 0xA000
BDOS_NEXT_ENTRY   = 0xA003
BDOS_FIND_FILE    = 0xA006
BDOS_READ_RECORD  = 0xA009
BDOS_DELETE_FILE  = 0xA00C
BDOS_CREATE_FILE  = 0xA00F
BDOS_WRITE_RECORD = 0xA012
BDOS_ENTRY_SIZE   = 0xA015
BDOS_RENAME_FILE  = 0xA018
BDOS_RUN_FILE     = 0xA01B
BDOS_FREE_BLOCKS  = 0xA01E

.area _CODE

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

; void bdos_rewind_dir(void)
_bdos_rewind_dir::
	jp	BDOS_REWIND_DIR

; unsigned char bdos_next_entry(void) - return value comes back in A
_bdos_next_entry::
	jp	BDOS_NEXT_ENTRY

; unsigned char bdos_find_file(void) - return value comes back in A
_bdos_find_file::
	jp	BDOS_FIND_FILE

; unsigned char bdos_read_record(void) - return value comes back in A
_bdos_read_record::
	jp	BDOS_READ_RECORD

; unsigned char bdos_delete_file(void) - return value comes back in A
_bdos_delete_file::
	jp	BDOS_DELETE_FILE

; unsigned char bdos_create_file(void) - return value comes back in A
_bdos_create_file::
	jp	BDOS_CREATE_FILE

; unsigned char bdos_write_record(void) - return value comes back in A
_bdos_write_record::
	jp	BDOS_WRITE_RECORD

; unsigned int bdos_entry_size(void) - return value comes back in HL
_bdos_entry_size::
	jp	BDOS_ENTRY_SIZE

; unsigned char bdos_rename_file(void) - return value comes back in A
_bdos_rename_file::
	jp	BDOS_RENAME_FILE

; unsigned char bdos_run_file(void) - return value comes back in A. Only
; ever returns at all if the named file wasn't found - see bdos.c's comment.
_bdos_run_file::
	jp	BDOS_RUN_FILE

; unsigned int bdos_free_blocks(void) - return value comes back in HL
_bdos_free_blocks::
	jp	BDOS_FREE_BLOCKS
