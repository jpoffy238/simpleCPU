/* C-facing declarations for BDOS: the filesystem logic (directory scan, find
 * file, read file content) that used to live inline in ccp.c, now moved to
 * its own ROM so CCP.COM - or any other program - can call it instead of
 * duplicating it. See bdos.c for the real implementation (bdos.c) or
 * bdos_glue.s (every other program) depending on which binary this header
 * ends up linked into.
 *
 * Calling convention, matching bios.h's "stage one value, then trigger"
 * style: every primitive below reuses bios_set_buffer()'s ADDRLO/ADDRHI
 * pair rather than introducing a second pointer ABI, just for a different
 * purpose depending on the call -
 *   bdos_find_file()   ADDRLO/ADDRHI = a null-terminated filename to search
 *                      for (input only - nothing is copied back out, since
 *                      the result is BDOS's own internal state, used by
 *                      bdos_read_record())
 *   bdos_next_entry()  ADDRLO/ADDRHI = a 32-byte destination for the next
 *                      directory entry (output)
 *   bdos_read_record() ADDRLO/ADDRHI = a 128-byte destination for the next
 *                      record of the file found by the last bdos_find_file()
 *   bdos_delete_file() ADDRLO/ADDRHI = a null-terminated filename to delete
 *   bdos_create_file() ADDRLO/ADDRHI = a null-terminated filename to create;
 *                      fails (returns 0) if it already exists or the
 *                      directory is full. Succeeds with a zero-length file,
 *                      same as real CP/M's F_MAKE - immediately visible to
 *                      bdos_next_entry()/bdos_find_file() even before any
 *                      bdos_write_record() call.
 *   bdos_write_record() ADDRLO/ADDRHI = a 128-byte source for the next
 *                      record of the file created by the last
 *                      bdos_create_file(). Rolls over to a new extent on
 *                      its own once the current one (16 blocks, 128
 *                      records) is full; fails (returns 0) only once the
 *                      directory or the disk itself is out of room.
 *   bdos_entry_size()  ADDRLO/ADDRHI = the 32-byte entry buffer
 *                      bdos_next_entry() just filled (no filename needed -
 *                      the name's already in there). Returns the file's
 *                      total record count across every extent sharing
 *                      that name, not just the one bdos_next_entry()
 *                      happened to return.
 *   bdos_rename_file() ADDRLO/ADDRHI = two consecutive null-terminated
 *                      strings, "NEWNAME\0OLDNAME\0" (matching real CP/M's
 *                      own "REN new=old" argument order) - a rename needs
 *                      two names and ADDRLO/ADDRHI only carries one
 *                      pointer, so the caller stages both in one buffer
 *                      rather than BDOS needing a second pointer ABI.
 * Every primitive above returns unsigned char (1 = succeeded/found, 0 =
 * not found/no more/failed) via cc65's standard single-byte return in A -
 * no different from bios_getch()'s existing return-a-value-in-A precedent -
 * except bdos_entry_size(), which returns unsigned int (a record count can
 * exceed 255) via the standard 16-bit A(low)/X(high) return. */
#ifndef BDOS_H
#define BDOS_H

void bdos_rewind_dir(void);
unsigned char bdos_next_entry(void);
unsigned char bdos_find_file(void);
unsigned char bdos_read_record(void);
unsigned char bdos_delete_file(void);
unsigned char bdos_create_file(void);
unsigned char bdos_write_record(void);
unsigned int bdos_entry_size(void);
unsigned char bdos_rename_file(void);

#endif
