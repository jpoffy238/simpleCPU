/* BDOS: the filesystem logic (directory scan, find file by name, read a
 * file's content record by record) - moved here out of ccp.c, ported
 * originally from CpmFileSystem.java (modules/Disk), which remains the
 * validated reference for this on-disk format.
 *
 * Lives in its own ROM, not loaded as part of a boot image the way CCP.COM
 * is - a boot image is capped at whatever fits in a few dozen sectors on
 * track 0 (moot now that B finds CCP.COM by name instead of loading a raw
 * image, but it's exactly the cap that motivated this move in the first
 * place), while ROM has no such budget. Every program that needs this logic
 * (CCP.COM today, whatever else later) calls it through a fixed jump table
 * (see bdos_jumptable.s) instead of duplicating it.
 *
 * Called two ways, both external to this compiled binary - BDOS.bin is never
 * linked together with a caller, since they're separate cl65 builds:
 *   - MONITOR.asm (hand assembly) JSRs straight to the fixed jump-table
 *     addresses.
 *   - Any cc65-compiled program (ccp.c, say) calls through bdos_glue.s's
 *     thin JSR stubs, declared in bdos.h - from that program's own C code,
 *     it looks like an ordinary function call.
 *
 * ADDRLO/ADDRHI (zero page $01/$02) is the same pointer-staging pair
 * bios_set_buffer() already uses for BIOS_READ_SECTOR/WRITE_SECTOR - reused
 * here for a different purpose depending on the call (input filename for
 * bdos_find_file(), output destination for bdos_next_entry()/
 * bdos_read_record()) rather than adding a second pointer ABI. TRANSFER
 * reads it back as an actual pointer value: on this little-endian platform,
 * the low byte at $01 and high byte at $02 is exactly how cc65 represents a
 * pointer itself, so treating address $01 as an "unsigned char **" and
 * dereferencing it reconstructs whatever bios_set_buffer() staged there.
 *
 * Stateful, matching how real CP/M's BDOS worked: bdos_rewind_dir()/
 * bdos_next_entry() track a directory scan position across calls, and
 * bdos_find_file()/bdos_read_record() track a "currently open file" (its
 * block list and remaining record count) across calls - both state blocks
 * are plain C statics here, there being only ever one caller active at a
 * time (BDOS is called like a subroutine, not run concurrently with
 * anything).
 *
 * main() is never actually entered - nothing JSRs to this ROM's reset-style
 * entry point, only to the jump table's individual slots - but cc65's crt0
 * startup unconditionally expects a _main symbol to link against, so an
 * unused stub satisfies the linker rather than fighting the toolchain's
 * standard build assumptions. */
#include "bios.h"

#define BOOT_TRACKS 1
#define SECTORS_PER_TRACK 26
#define SECTORS_PER_BLOCK 8
#define DIR_SECTORS 16
#define ENTRIES_PER_SECTOR 4
#define ENTRY_SIZE 32
#define STATUS_EMPTY 0xE5
#define DIRECTORY_BLOCKS 2
#define MAX_BLOCKS_PER_FILE 16
/* (76 - BOOT_TRACKS) tracks * 26 sectors / SECTORS_PER_BLOCK, floor -
 * matches CpmFileSystem.totalBlocks(). Blocks 0 and 1 (DIRECTORY_BLOCKS)
 * are always "used" by the directory itself; a block pointer of 0 in an
 * FCB means "unused", so 0 doubles as next_free_block()'s "none left"
 * sentinel - consistent with that convention. */
#define TOTAL_BLOCKS 243

#define TRANSFER (*(unsigned char **) 0x0001)

static unsigned char sector_buf[128];

/* directory scan position (bdos_rewind_dir/bdos_next_entry) */
static unsigned char dir_sector_index;
static unsigned char dir_entry_index;

/* the "currently open file" (bdos_find_file/bdos_read_record) - one extent's
 * worth of state at a time, not every extent's up front, even for a
 * multi-extent file: open_extent/open_extent_full track which extent this
 * is and whether it was full (128 records), so bdos_read_record() knows
 * whether to go looking for open_extent+1 once open_records_left hits 0, or
 * whether that's genuinely the end of the file. */
static unsigned char open_blocks[16];
static unsigned int open_records_left;
static unsigned char open_block_index;
static unsigned char open_sector_in_block;
static unsigned char open_extent;
static unsigned char open_extent_full;

/* the "currently being written" file (bdos_create_file/bdos_write_record) -
 * kept entirely separate from the read-side state above, even though CCP
 * never has both open at once in practice, so that a future program doing
 * a real read-and-write-at-once operation (unlike COPY, which fully reads
 * before it starts writing) wouldn't have the two interfere. write_entry
 * mirrors the on-disk 32-byte FCB format directly, so writing it out is a
 * straight copy rather than a field-by-field reassembly. */
static unsigned char write_entry[ENTRY_SIZE];
static unsigned char write_dir_sector;
static unsigned char write_dir_offset;
static unsigned char write_block_count;
static unsigned char write_active;
static unsigned char write_extent;

/* 1 bit per block, covering blocks 0-255 (TOTAL_BLOCKS is 243) - built
 * fresh by bdos_create_file() from a full directory scan, then kept in
 * sync as bdos_write_record() allocates more blocks for the same file. */
static unsigned char block_bitmap[32];

/* File-scope rather than a local inside bdos_find_file() deliberately:
 * B's new boot sequence calls bdos_find_file() before any program's own
 * crt0 has ever run, meaning cc65's software stack pointer (sp, zero page)
 * is not yet initialized by anything - a local array would be allocated on
 * that stack. Keeping every local here a plain scalar (handled via
 * registers/zero-page temporaries, not the software stack) avoids the
 * question entirely rather than relying on an uninitialized sp happening to
 * be harmless. */
static unsigned char fcb_name[11];
/* bdos_rename_file()'s destination name - a second static for the same
 * uninitialized-sp-at-boot reason as fcb_name, even though rename is
 * never actually called at boot time; keeping both as statics (rather
 * than fcb_name plus a local for the other) is simpler than reasoning
 * about which of the two names is "the boot-sensitive one." */
static unsigned char new_fcb_name[11];

/* Port of CpmFileSystem.readDataSector() - divides/mods by
 * SECTORS_PER_TRACK, which the 6502 has no native instruction for. */
static void read_data_sector(unsigned int index) {
	bios_set_buffer(sector_buf);
	bios_set_x(1 + index % SECTORS_PER_TRACK);
	bios_read_sector(BOOT_TRACKS + index / SECTORS_PER_TRACK);
}

/* Mirror of read_data_sector() for the write direction - writes sector_buf's
 * current content out, same (track, sector) mapping. */
static void write_data_sector(unsigned int index) {
	bios_set_buffer(sector_buf);
	bios_set_x(1 + index % SECTORS_PER_TRACK);
	bios_write_sector(BOOT_TRACKS + index / SECTORS_PER_TRACK);
}

static void mark_block_used(unsigned char block) {
	block_bitmap[block >> 3] |= (1 << (block & 7));
}

static unsigned char block_is_used(unsigned char block) {
	return block_bitmap[block >> 3] & (1 << (block & 7));
}

/* First block at or past DIRECTORY_BLOCKS not yet marked used, or 0 (never
 * a valid data block - see TOTAL_BLOCKS's comment) if the disk is full. */
static unsigned char next_free_block(void) {
	unsigned char block;
	for (block = DIRECTORY_BLOCKS; block < TOTAL_BLOCKS; block++) {
		if (!block_is_used(block)) {
			return block;
		}
	}
	return 0;
}

/* Read-modify-write: reloads the sector (to preserve its other 3 entries),
 * overwrites just this one's 32 bytes with write_entry, writes it back. */
static void write_directory_entry(unsigned char sector_index, unsigned char offset_in_sector) {
	unsigned char i;
	read_data_sector(sector_index);
	for (i = 0; i < ENTRY_SIZE; i++) {
		sector_buf[offset_in_sector * ENTRY_SIZE + i] = write_entry[i];
	}
	write_data_sector(sector_index);
}

/* Port of CpmFileSystem.encodeFileName() - "NAME" or "NAME.EXT" into the
 * 11-byte, space-padded, uppercase FCB name format. Silently truncates a
 * too-long name/extension rather than rejecting it. */
static void encode_name(const char *filename, unsigned char *out_name) {
	unsigned char i;
	char c;
	for (i = 0; i < 11; i++) {
		out_name[i] = ' ';
	}
	i = 0;
	while (*filename != '\0' && *filename != '.' && i < 8) {
		c = *filename++;
		if (c >= 'a' && c <= 'z') {
			c -= ('a' - 'A');
		}
		out_name[i++] = c;
	}
	while (*filename != '\0' && *filename != '.') {
		filename++;
	}
	if (*filename == '.') {
		filename++;
		i = 0;
		while (*filename != '\0' && i < 3) {
			c = *filename++;
			if (c >= 'a' && c <= 'z') {
				c -= ('a' - 'A');
			}
			out_name[8 + i++] = c;
		}
	}
}

void bdos_rewind_dir(void) {
	dir_sector_index = 0;
	dir_entry_index = 0;
}

/* Advances to the next non-empty, first-extent (EX == 0) directory entry -
 * skipping STATUS_EMPTY ones and every extent past the first internally,
 * so a caller never needs to check for either - otherwise a multi-extent
 * file would show up once per extent instead of once. Copies the match
 * into the caller's ADDRLO/ADDRHI-staged 32-byte buffer. Returns 0 once
 * the whole directory has been scanned. */
unsigned char bdos_next_entry(void) {
	unsigned char *entry;
	/* Captured before any read_data_sector() call below, not after - that
	 * call stages its own sector_buf pointer via bios_set_buffer(), which
	 * overwrites the very same ADDRLO/ADDRHI the caller staged its
	 * destination buffer into. Reading TRANSFER too late would silently
	 * hand back BDOS's own sector buffer instead of the caller's. */
	unsigned char *dest = TRANSFER;
	unsigned char i;

	for (;;) {
		if (dir_sector_index >= DIR_SECTORS) {
			return 0;
		}
		if (dir_entry_index == 0) {
			read_data_sector(dir_sector_index);
		}
		entry = sector_buf + dir_entry_index * ENTRY_SIZE;
		dir_entry_index++;
		if (dir_entry_index >= ENTRIES_PER_SECTOR) {
			dir_entry_index = 0;
			dir_sector_index++;
		}
		if (entry[0] != STATUS_EMPTY && entry[12] == 0) {
			for (i = 0; i < ENTRY_SIZE; i++) {
				dest[i] = entry[i];
			}
			return 1;
		}
	}
}

/* Scans the whole directory for fcb_name (already encoded) at exactly the
 * given extent number. On a match, loads open_blocks/open_records_left/
 * open_extent_full from it (resetting the block/sector read position) and
 * returns 1; returns 0 if that file/extent combination doesn't exist. */
static unsigned char find_extent(unsigned char extent) {
	unsigned char sector_index, entry_index, i;
	unsigned char *entry;

	for (sector_index = 0; sector_index < DIR_SECTORS; sector_index++) {
		read_data_sector(sector_index);
		for (entry_index = 0; entry_index < ENTRIES_PER_SECTOR; entry_index++) {
			entry = sector_buf + entry_index * ENTRY_SIZE;
			if (entry[0] == STATUS_EMPTY || entry[12] != extent) {
				continue;
			}
			for (i = 0; i < 11 && entry[1 + i] == fcb_name[i]; i++) {
				/* comparing */
			}
			if (i == 11) {
				for (i = 0; i < 16; i++) {
					open_blocks[i] = entry[16 + i];
				}
				open_records_left = entry[15];
				open_extent_full = (entry[15] == 128);
				open_block_index = 0;
				open_sector_in_block = 0;
				open_extent = extent;
				return 1;
			}
		}
	}
	return 0;
}

/* Finds the filename staged at ADDRLO/ADDRHI's first extent (EX == 0) -
 * a multi-extent file's 2nd+ extent is found later, by bdos_read_record()
 * itself once it runs out of this one's records. Remembers the entry's
 * block list and record count for bdos_read_record() and returns 1; returns
 * 0 if not found. Independent of bdos_next_entry()'s scan position - this
 * always does its own complete scan in one call, rather than sharing/
 * disturbing that other iterator. */
unsigned char bdos_find_file(void) {
	const char *filename = (const char *) TRANSFER;

	encode_name(filename, fcb_name);
	return find_extent(0);
}

/* Sums the record count across every extent sharing the 11-byte FCB name
 * already present in the caller's ADDRLO/ADDRHI-staged 32-byte entry
 * buffer - the same buffer bdos_next_entry() just filled, so a DIR-style
 * caller doesn't need to separately encode/stage a filename string at
 * all. Reuses find_extent() to do the actual walking (same "was this
 * extent full" signal bdos_read_record() uses to know whether to look for
 * the next one) - this does leave bdos_find_file()/bdos_read_record()'s
 * "currently open for reading" state pointed at whatever extent this call
 * last examined, which is fine for DIR (it never also has a file open for
 * reading at the same time) but worth knowing if a future caller wants
 * both at once. Returns the total via the standard 16-bit A(low)/X(high)
 * return - a record count can exceed 255 for a big enough file, so this
 * can't fit in the usual single-byte A return the other primitives use. */
unsigned int bdos_entry_size(void) {
	unsigned char *entry_buf = TRANSFER;
	unsigned char i;
	unsigned int total = 0;
	unsigned char extent;

	for (i = 0; i < 11; i++) {
		fcb_name[i] = entry_buf[1 + i];
	}

	for (extent = 0; find_extent(extent); extent++) {
		total += open_records_left;
		if (!open_extent_full) {
			break;
		}
	}
	return total;
}

/* Reads the next 128-byte record of the file found by the last successful
 * bdos_find_file() call into the caller's ADDRLO/ADDRHI-staged buffer.
 * Transparently crosses from one extent into the next (open_extent + 1)
 * when the current one is exhausted but was full - real CP/M's own
 * signal that there's more file beyond this extent. Returns 0 once the
 * file's record count (or its block list) is exhausted for real. */
unsigned char bdos_read_record(void) {
	unsigned char block;
	unsigned int base_sector;
	/* Captured before read_data_sector() below - see bdos_next_entry()'s
	 * comment above; same ADDRLO/ADDRHI-clobbering hazard applies here
	 * (find_extent(), called below on an extent boundary, has the same
	 * hazard too - dest is captured before either). */
	unsigned char *dest = TRANSFER;
	unsigned char i;

	if (open_records_left == 0) {
		if (!open_extent_full || !find_extent(open_extent + 1)) {
			return 0;
		}
	}
	block = open_blocks[open_block_index];
	if (block == 0) {
		open_records_left = 0;
		return 0;
	}

	base_sector = (unsigned int) block * SECTORS_PER_BLOCK;
	read_data_sector(base_sector + open_sector_in_block);

	for (i = 0; i < 128; i++) {
		dest[i] = sector_buf[i];
	}

	open_records_left--;
	open_sector_in_block++;
	if (open_sector_in_block >= SECTORS_PER_BLOCK) {
		open_sector_in_block = 0;
		open_block_index++;
	}
	return 1;
}

/* Scans the whole directory for fcb_name (already encoded) at exactly the
 * given extent number; on a match, marks it empty and writes that sector
 * back. Returns the entry's record count (0-128) if found, or 0xFF (never
 * a valid record count) if that file/extent combination doesn't exist. */
static unsigned char delete_extent(unsigned char extent) {
	unsigned char sector_index, entry_index, i;
	unsigned char *entry;
	unsigned char records;

	for (sector_index = 0; sector_index < DIR_SECTORS; sector_index++) {
		read_data_sector(sector_index);
		for (entry_index = 0; entry_index < ENTRIES_PER_SECTOR; entry_index++) {
			entry = sector_buf + entry_index * ENTRY_SIZE;
			if (entry[0] == STATUS_EMPTY || entry[12] != extent) {
				continue;
			}
			for (i = 0; i < 11 && entry[1 + i] == fcb_name[i]; i++) {
				/* comparing */
			}
			if (i == 11) {
				records = entry[15];
				entry[0] = STATUS_EMPTY;
				write_data_sector(sector_index);
				return records;
			}
		}
	}
	return 0xFF;
}

/* Deletes every extent of the filename staged at ADDRLO/ADDRHI - walks
 * extent 0, 1, 2... until one isn't full (the real end of the file) or
 * isn't found at all. Returns 1 if anything was deleted, 0 if not found.
 * Data blocks aren't touched - a block only counts as "used" by virtue of
 * being referenced from a non-empty entry (see next_free_block()/
 * bdos_create_file()'s directory scan), so clearing the entries frees
 * them implicitly. */
unsigned char bdos_delete_file(void) {
	const char *filename = (const char *) TRANSFER;
	unsigned char extent;
	unsigned char records;
	unsigned char found_any = 0;

	encode_name(filename, fcb_name);

	for (extent = 0; ; extent++) {
		records = delete_extent(extent);
		if (records == 0xFF) {
			break;
		}
		found_any = 1;
		if (records < 128) {
			break;
		}
	}
	return found_any;
}

/* Scans the whole directory for fcb_name (already encoded) at exactly the
 * given extent number; on a match, overwrites its name bytes with
 * new_fcb_name and writes that sector back. Returns the entry's record
 * count (0-128), or 0xFF if that file/extent combination doesn't exist -
 * same convention as delete_extent(). */
static unsigned char rename_extent(unsigned char extent) {
	unsigned char sector_index, entry_index, i;
	unsigned char *entry;
	unsigned char records;

	for (sector_index = 0; sector_index < DIR_SECTORS; sector_index++) {
		read_data_sector(sector_index);
		for (entry_index = 0; entry_index < ENTRIES_PER_SECTOR; entry_index++) {
			entry = sector_buf + entry_index * ENTRY_SIZE;
			if (entry[0] == STATUS_EMPTY || entry[12] != extent) {
				continue;
			}
			for (i = 0; i < 11 && entry[1 + i] == fcb_name[i]; i++) {
				/* comparing */
			}
			if (i == 11) {
				records = entry[15];
				for (i = 0; i < 11; i++) {
					entry[1 + i] = new_fcb_name[i];
				}
				write_data_sector(sector_index);
				return records;
			}
		}
	}
	return 0xFF;
}

/* Renames every extent of a file. ADDRLO/ADDRHI points at two consecutive
 * null-terminated strings, "NEWNAME\0OLDNAME\0" - matching real CP/M's own
 * "REN new=old" argument order - since a rename genuinely needs two names
 * and ADDRLO/ADDRHI only carries one pointer; the caller builds this by
 * splitting its command line on '=' in place, the same trick ccp.c's
 * cmd_copy() already uses on a space for COPY's two names. Fails (0) if
 * the old name doesn't exist or the new name already does - never leaves
 * a half-renamed set of extents in that case, since the collision check
 * happens before any entry is touched. */
unsigned char bdos_rename_file(void) {
	const char *names = (const char *) TRANSFER;
	const char *old_filename;
	unsigned char sector_index, entry_index, i;
	unsigned char *entry;
	unsigned char extent;
	unsigned char records;
	unsigned char found_any = 0;

	encode_name(names, new_fcb_name);
	old_filename = names;
	while (*old_filename != '\0') {
		old_filename++;
	}
	old_filename++;
	encode_name(old_filename, fcb_name);

	for (sector_index = 0; sector_index < DIR_SECTORS; sector_index++) {
		read_data_sector(sector_index);
		for (entry_index = 0; entry_index < ENTRIES_PER_SECTOR; entry_index++) {
			entry = sector_buf + entry_index * ENTRY_SIZE;
			if (entry[0] == STATUS_EMPTY) {
				continue;
			}
			for (i = 0; i < 11 && entry[1 + i] == new_fcb_name[i]; i++) {
				/* comparing */
			}
			if (i == 11) {
				return 0; /* new name already exists */
			}
		}
	}

	for (extent = 0; ; extent++) {
		records = rename_extent(extent);
		if (records == 0xFF) {
			break;
		}
		found_any = 1;
		if (records < 128) {
			break;
		}
	}
	return found_any;
}

/* Creates a new, zero-length file named by ADDRLO/ADDRHI, immediately
 * visible to bdos_next_entry()/bdos_find_file() even before any
 * bdos_write_record() call - same as real CP/M's F_MAKE. Fails (0) if the
 * name already exists or the directory has no free entry. One directory
 * scan does triple duty: checks for an existing name, finds a free entry
 * to use, and builds the used-block bitmap bdos_write_record() will
 * allocate from - independent of bdos_find_file()'s "currently open for
 * reading" state, so nothing stops a program from having both open at
 * once (CCP's COPY never does, but BDOS doesn't assume that). */
unsigned char bdos_create_file(void) {
	unsigned char sector_index, entry_index, i;
	unsigned char *entry;
	const char *filename = (const char *) TRANSFER;
	unsigned char found_free = 0;
	unsigned char free_sector = 0;
	unsigned char free_offset = 0;

	encode_name(filename, fcb_name);

	for (i = 0; i < 32; i++) {
		block_bitmap[i] = 0;
	}
	mark_block_used(0);
	mark_block_used(1);

	for (sector_index = 0; sector_index < DIR_SECTORS; sector_index++) {
		read_data_sector(sector_index);
		for (entry_index = 0; entry_index < ENTRIES_PER_SECTOR; entry_index++) {
			entry = sector_buf + entry_index * ENTRY_SIZE;
			if (entry[0] == STATUS_EMPTY) {
				if (!found_free) {
					found_free = 1;
					free_sector = sector_index;
					free_offset = entry_index;
				}
				continue;
			}
			for (i = 0; i < 11 && entry[1 + i] == fcb_name[i]; i++) {
				/* comparing */
			}
			if (i == 11) {
				return 0; /* already exists */
			}
			for (i = 0; i < 16; i++) {
				if (entry[16 + i] != 0) {
					mark_block_used(entry[16 + i]);
				}
			}
		}
	}

	if (!found_free) {
		return 0; /* directory full */
	}

	for (i = 0; i < ENTRY_SIZE; i++) {
		write_entry[i] = 0;
	}
	for (i = 0; i < 11; i++) {
		write_entry[1 + i] = fcb_name[i];
	}
	write_dir_sector = free_sector;
	write_dir_offset = free_offset;
	write_block_count = 0;
	write_extent = 0;
	write_active = 1;

	write_directory_entry(write_dir_sector, write_dir_offset);
	return 1;
}

/* Starts a new extent (EX = write_extent + 1) for the file currently being
 * written, once the current one (16 blocks, 128 records) is full - a fresh
 * directory scan finds a free entry (not assumed from bdos_create_file()'s
 * original scan, which may be stale by now), write_entry's block-pointer
 * region is cleared and its record count reset to 0, and the new entry is
 * written to disk immediately, same as bdos_create_file()'s first extent.
 * Returns 1 if a free entry was found, 0 if the directory is full. */
static unsigned char start_new_extent(void) {
	unsigned char sector_index, entry_index, i;
	unsigned char *entry;

	for (sector_index = 0; sector_index < DIR_SECTORS; sector_index++) {
		read_data_sector(sector_index);
		for (entry_index = 0; entry_index < ENTRIES_PER_SECTOR; entry_index++) {
			entry = sector_buf + entry_index * ENTRY_SIZE;
			if (entry[0] != STATUS_EMPTY) {
				continue;
			}
			write_dir_sector = sector_index;
			write_dir_offset = entry_index;
			write_extent++;
			write_entry[12] = write_extent;
			write_entry[15] = 0;
			for (i = 0; i < 16; i++) {
				write_entry[16 + i] = 0;
			}
			write_block_count = 0;
			write_directory_entry(write_dir_sector, write_dir_offset);
			return 1;
		}
	}
	return 0;
}

/* Writes the next 128-byte record (staged at ADDRLO/ADDRHI) to the file
 * created by the last successful bdos_create_file(), allocating a new
 * block from block_bitmap whenever the record count crosses an 8-record
 * (1KB) boundary, and a whole new extent (via start_new_extent()) once the
 * current one's 16 blocks (128 records) are full - transparent to the
 * caller, same as bdos_read_record() crossing an extent boundary the other
 * direction. Rewrites the directory entry after every record rather than
 * needing a separate "close" call - simpler, and crash-resilient (a
 * mid-write abort leaves a file a few records short, never a corrupt
 * entry pointing at garbage blocks). Fails (0) once the directory or the
 * disk itself is full. */
unsigned char bdos_write_record(void) {
	/* Captured before write_directory_entry()'s internal read_data_sector()
	 * call - same ADDRLO/ADDRHI-clobbering hazard as bdos_read_record()
	 * (start_new_extent(), called below on an extent boundary, has the
	 * same hazard too - src is captured before either). */
	unsigned char *src = TRANSFER;
	unsigned char block;
	unsigned char block_index, sector_in_block;
	unsigned int base_sector;
	unsigned char i;

	if (!write_active) {
		return 0;
	}

	if (write_entry[15] >= 128) {
		/* current extent is full - start a new one before writing anything,
		 * so block_index/sector_in_block below are computed fresh against
		 * write_entry[15] == 0 rather than patched after the fact */
		if (!start_new_extent()) {
			return 0; /* directory full */
		}
	}

	block_index = write_entry[15] / SECTORS_PER_BLOCK;
	sector_in_block = write_entry[15] % SECTORS_PER_BLOCK;

	if (sector_in_block == 0) {
		block = next_free_block();
		if (block == 0) {
			return 0; /* disk full */
		}
		mark_block_used(block);
		write_entry[16 + write_block_count] = block;
		write_block_count++;
	}

	block = write_entry[16 + block_index];
	base_sector = (unsigned int) block * SECTORS_PER_BLOCK;

	for (i = 0; i < 128; i++) {
		sector_buf[i] = src[i];
	}
	write_data_sector(base_sector + sector_in_block);

	write_entry[15]++;
	write_directory_entry(write_dir_sector, write_dir_offset);
	return 1;
}

int main(void) {
	return 0; /* unreachable - see the file comment above */
}
