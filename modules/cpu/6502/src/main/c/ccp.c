/* A minimal CP/M-style CCP (Console Command Processor): the shell loop,
 * drive switching (A:, B:, ...), DIR, TYPE, ERA, COPY, REN, and SAVE. The
 * filesystem logic these commands need (directory scan, find file, read/
 * write file content) used to live right here, ported from
 * CpmFileSystem.java - it now lives in BDOS (bdos.c), called through
 * bdos.h/bdos_glue.s, since ROM isn't boot-image-size-constrained the way
 * this program is, and any future program needing the same logic
 * shouldn't have to duplicate it either.
 *
 * Unlike hello.c, this never returns - a real shell takes over the machine
 * the way a booted DOS is meant to (B/the monitor is a sequential handoff,
 * not something a shell hands control back to after one line). */
#include "bios.h"
#include "bdos.h"

#define NUM_DRIVES 4
#define LINELEN 40
#define ENTRY_SIZE 32

static unsigned char current_drive = 0;
static char line[LINELEN];

static void print_string(const char *s) {
	while (*s != '\0') {
		bios_putch(*s++);
	}
}

static void print_prompt(void) {
	bios_putch('A' + current_drive);
	bios_putch('>');
}

/* Port of MONITOR.asm's READLINE: echoes as it goes, backspace/DEL erase the
 * last character (visually erasing via BS,space,BS same as a real dumb
 * terminal), CR or LF ends the line. Returns the line length. */
static unsigned char read_line(void) {
	unsigned char len = 0;
	char c;
	for (;;) {
		c = bios_getch();
		if (c == '\r' || c == '\n') {
			break;
		}
		if (c == '\b' || c == 0x7F) {
			if (len > 0) {
				len--;
				bios_putch('\b');
				bios_putch(' ');
				bios_putch('\b');
			}
			continue;
		}
		if (len < LINELEN - 1) {
			line[len++] = c;
			bios_putch(c);
		}
	}
	line[len] = '\0';
	bios_putch('\n');
	return len;
}

/* Recognizes a bare drive letter + colon ("A:", "b:", ...) and nothing else -
 * matching real CP/M, where typing a drive letter alone at the CCP prompt
 * changes the current drive without a separate command name. Returns 1 if
 * the line was a drive switch (valid or not - either way, not an unknown
 * command), 0 otherwise. */
static unsigned char try_drive_switch(unsigned char len) {
	char letter;
	if (len != 2 || line[1] != ':') {
		return 0;
	}
	letter = line[0];
	if (letter >= 'a' && letter <= 'z') {
		letter -= ('a' - 'A');
	}
	if (letter < 'A' || letter >= 'A' + NUM_DRIVES) {
		return 0;
	}
	current_drive = letter - 'A';
	bios_select(current_drive);
	return 1;
}

/* Recognizes an optional "X:" drive prefix at the start of a filename -
 * same drive-letter convention as try_drive_switch's bare "X:" at the CCP
 * prompt, and real CP/M's own d:filename syntax - so COPY can name a drive
 * other than the current one on either side. On a match, writes the drive
 * index to *drive and returns a pointer past the prefix; with no prefix,
 * *drive is set to current_drive and name is returned unchanged. A prefix
 * whose letter is out of range sets *drive to 0xFF (never a valid drive
 * index - see NUM_DRIVES), so the caller can tell "bad prefix" apart from
 * "no prefix" without a second return channel. */
static char *parse_drive_prefix(char *name, unsigned char *drive) {
	char letter;
	*drive = current_drive;
	if (name[0] == '\0' || name[1] != ':') {
		return name;
	}
	letter = name[0];
	if (letter >= 'a' && letter <= 'z') {
		letter -= ('a' - 'A');
	}
	if (letter < 'A' || letter >= 'A' + NUM_DRIVES) {
		*drive = 0xFF;
		return name;
	}
	*drive = letter - 'A';
	return name + 2;
}

/* Case-insensitive exact match of line against word - not a prefix match,
 * so "DIRT" doesn't match "DIR". */
static unsigned char matches(const char *word) {
	unsigned char i;
	char c;
	for (i = 0; word[i] != '\0'; i++) {
		c = line[i];
		if (c >= 'a' && c <= 'z') {
			c -= ('a' - 'A');
		}
		if (c != word[i]) {
			return 0;
		}
	}
	return line[i] == '\0';
}

/* Like matches(), but for a command that takes an argument: checks line
 * starts with `word` followed by a space (case-insensitive), and returns
 * the index where the argument starts, or 0 if word doesn't match (0 is
 * never a valid argument start, since word is never empty). */
static unsigned char starts_with(const char *word) {
	unsigned char i;
	char c;
	for (i = 0; word[i] != '\0'; i++) {
		c = line[i];
		if (c >= 'a' && c <= 'z') {
			c -= ('a' - 'A');
		}
		if (c != word[i]) {
			return 0;
		}
	}
	if (line[i] != ' ') {
		return 0;
	}
	return i + 1;
}

/* Prints one directory entry's name, space-padded trailing bytes trimmed,
 * as NAME or NAME.EXT. entry points at a 32-byte FCB copied out by
 * bdos_next_entry(). */
static void print_entry_name(const unsigned char *entry) {
	unsigned char i;
	for (i = 0; i < 8 && entry[1 + i] != ' '; i++) {
		bios_putch(entry[1 + i]);
	}
	if (entry[9] != ' ') {
		bios_putch('.');
		for (i = 0; i < 3 && entry[9 + i] != ' '; i++) {
			bios_putch(entry[9 + i]);
		}
	}
	bios_putch(' ');
}

/* Nothing in this file prints a number yet - bios_putch only does single
 * characters - so a file's size needs its own decimal-digit conversion.
 * Standard technique: peel off low-order digits via %10/10 (collecting
 * them backwards, since that's the only order division gives them up in),
 * then print the buffer in reverse. 5 digits comfortably covers any
 * unsigned int (max "65535"). */
static void print_decimal(unsigned int value) {
	char digits[5];
	unsigned char count = 0;
	if (value == 0) {
		bios_putch('0');
		return;
	}
	while (value > 0) {
		digits[count++] = '0' + (value % 10);
		value /= 10;
	}
	while (count > 0) {
		bios_putch(digits[--count]);
	}
}

static void cmd_dir(void) {
	unsigned char entry[ENTRY_SIZE];
	unsigned char found = 0;
	unsigned int records;

	bdos_rewind_dir();
	for (;;) {
		/* Restaged every iteration, not just once before the loop -
		 * bdos_next_entry() calls BIOS_READ_SECTOR internally to pull the
		 * next directory sector, which stages its own buffer into this
		 * same ADDRLO/ADDRHI pair first. */
		bios_set_buffer(entry);
		if (!bdos_next_entry()) {
			break;
		}
		print_entry_name(entry);
		bios_set_buffer(entry); /* restaged again - same reason, before bdos_entry_size() */
		records = bdos_entry_size();
		print_decimal((records + 7) / 8); /* records * 128 bytes / 1024 bytes-per-k, done
		                                    * as a division by 8 instead of a multiply-then-
		                                    * divide, since 128/1024 reduces to 1/8 exactly -
		                                    * avoids overflowing unsigned int on a big file */
		print_string("k ");
		found = 1;
	}
	if (!found) {
		print_string("NO FILE");
	}
	bios_putch('\n');
}

/* Prints a file's contents, one BDOS-supplied 128-byte record at a time.
 * Lengths are only tracked to the nearest 128-byte record, same real
 * limitation as CpmFileSystem/real CP/M - a file whose length isn't a
 * multiple of 128 prints trailing padding (zero, for a freshly-formatted
 * disk) past its actual content. */
static void cmd_type(const char *filename) {
	unsigned char record[128];
	unsigned char j;

	bios_set_buffer((void *) filename);
	if (!bdos_find_file()) {
		print_string("NOT FOUND\n");
		return;
	}

	for (;;) {
		bios_set_buffer(record);
		if (!bdos_read_record()) {
			break;
		}
		for (j = 0; j < 128; j++) {
			bios_putch(record[j]);
		}
	}
}

static void cmd_era(const char *filename) {
	bios_set_buffer((void *) filename);
	if (!bdos_delete_file()) {
		print_string("NOT FOUND\n");
	}
}

/* Finds the first space in line at or after `from`, returning its index, or
 * 0 if none (0 is never a valid split point here, since `from` is always
 * past a command word like "COPY "/"SAVE " itself). Doesn't handle runs of
 * multiple spaces specially - same "fine for this first pass" tolerance as
 * encode_name's. */
static unsigned char find_space(unsigned char from) {
	unsigned char i;
	for (i = from; line[i] != '\0'; i++) {
		if (line[i] == ' ') {
			return i;
		}
	}
	return 0;
}

/* Parses a run of decimal digits starting at line[from] into *out, stopping
 * at the first non-digit (typically the space before a command's next
 * argument, or the line's end) - nothing in this file has needed to parse
 * a number before now, everything else being hex-free filenames. Returns 1
 * if at least one digit was found, 0 otherwise. */
static unsigned char parse_decimal(unsigned char from, unsigned int *out) {
	unsigned int value = 0;
	unsigned char any = 0;
	while (line[from] >= '0' && line[from] <= '9') {
		value = value * 10 + (line[from] - '0');
		from++;
		any = 1;
	}
	*out = value;
	return any;
}

/* Reads the source file's content (via the same BDOS primitives TYPE uses)
 * and writes it to a new file under the destination name (via
 * bdos_create_file()/bdos_write_record()). The two files' BDOS state
 * (open_* for reading, write_* for writing) are independent - see bdos.c -
 * so interleaving one bdos_read_record()/bdos_write_record() pair per
 * record, rather than reading the whole source into memory first, is safe.
 *
 * Either name may carry an "X:" prefix (parse_drive_prefix()) naming a
 * drive other than the current one. Drive selection is a single global
 * BIOS register, not per-file, so every BDOS call below is preceded by an
 * explicit bios_select() for whichever side (source or dest) it touches -
 * redundant when both names share a drive (the common case), but no
 * costlier than that, since BIOS_SELECT just stores a register rather than
 * seeking. current_drive itself (the CCP prompt's own notion of "the"
 * drive) is restored before every return, so COPY never leaves the BIOS
 * pointed at whichever drive it last touched. */
static void cmd_copy(unsigned char arg_start) {
	unsigned char space_index;
	char *src, *dest;
	unsigned char src_drive, dst_drive;
	unsigned char record[128];

	space_index = find_space(arg_start);
	if (space_index == 0) {
		bios_putch('?');
		bios_putch('\n');
		return;
	}
	dest = &line[space_index + 1];
	line[space_index] = '\0'; /* split: line[arg_start..] is now just the source name */
	src = &line[arg_start];

	src = parse_drive_prefix(src, &src_drive);
	dest = parse_drive_prefix(dest, &dst_drive);
	if (src_drive == 0xFF || dst_drive == 0xFF) {
		bios_putch('?');
		bios_putch('\n');
		return;
	}

	bios_select(src_drive);
	bios_set_buffer(src);
	if (!bdos_find_file()) {
		print_string("NOT FOUND\n");
		bios_select(current_drive);
		return;
	}

	bios_select(dst_drive);
	bios_set_buffer(dest);
	if (!bdos_create_file()) {
		print_string("CANNOT CREATE\n");
		bios_select(current_drive);
		return;
	}

	for (;;) {
		bios_select(src_drive);
		bios_set_buffer(record);
		if (!bdos_read_record()) {
			break;
		}
		bios_select(dst_drive);
		bios_set_buffer(record);
		if (!bdos_write_record()) {
			print_string("DISK FULL\n");
			break;
		}
	}
	bios_select(current_drive);
}

/* REN newname=oldname - real CP/M's own argument order (new name first)
 * and separator ('=', not a space) - matches bdos_rename_file()'s
 * "NEWNAME\0OLDNAME\0" staging convention directly, so the only work here
 * is turning the '=' into the '\0' between them. */
static void cmd_ren(unsigned char arg_start) {
	unsigned char i;
	for (i = arg_start; line[i] != '\0'; i++) {
		if (line[i] == '=') {
			line[i] = '\0';
			bios_set_buffer(&line[arg_start]);
			if (!bdos_rename_file()) {
				print_string("CANNOT RENAME\n");
			}
			return;
		}
	}
	bios_putch('?');
	bios_putch('\n');
}

/* SAVE <pages> <filename> - real CP/M syntax: write <pages> * 256 bytes
 * starting at the TPA base to a new file. Real CP/M's TPA starts at
 * $0100; this system's equivalent is BOOTADDR ($0300), where B loads
 * CCP.COM itself - there's no separate loader yet that puts anything
 * else there, so for now SAVE just snapshots CCP.COM's own running
 * image, not "a program you loaded and tweaked" the way real CP/M's
 * typically would. Writes straight from memory to disk with no copy
 * buffer needed - bdos_write_record() only needs ADDRLO/ADDRHI pointed
 * at 128 bytes to read, and memory qualifies exactly as well as a local
 * array would. */
static void cmd_save(unsigned char arg_start) {
	unsigned char space_index;
	unsigned int pages;
	unsigned int records;
	unsigned int i;
	unsigned char *addr;

	space_index = find_space(arg_start);
	if (space_index == 0 || !parse_decimal(arg_start, &pages)) {
		bios_putch('?');
		bios_putch('\n');
		return;
	}

	bios_set_buffer(&line[space_index + 1]);
	if (!bdos_create_file()) {
		print_string("CANNOT CREATE\n");
		return;
	}

	records = pages * 2; /* 1 page = 256 bytes = 2 128-byte records */
	addr = (unsigned char *) 0x0300; /* BOOTADDR - see the comment above */
	for (i = 0; i < records; i++) {
		bios_set_buffer(addr);
		if (!bdos_write_record()) {
			print_string("DISK FULL\n");
			break;
		}
		addr += 128;
	}
}

int main(void) {
	unsigned char len;
	unsigned char arg_start;

	bios_select(current_drive); /* explicit - don't rely on whatever B's
	                              * caller left the drive selection as */

	for (;;) {
		print_prompt();
		len = read_line();
		if (len == 0) {
			continue;
		}
		if (matches("DIR")) {
			cmd_dir();
		} else if ((arg_start = starts_with("TYPE")) != 0) {
			cmd_type(&line[arg_start]);
		} else if ((arg_start = starts_with("ERA")) != 0) {
			cmd_era(&line[arg_start]);
		} else if ((arg_start = starts_with("COPY")) != 0) {
			cmd_copy(arg_start);
		} else if ((arg_start = starts_with("REN")) != 0) {
			cmd_ren(arg_start);
		} else if ((arg_start = starts_with("SAVE")) != 0) {
			cmd_save(arg_start);
		} else if (!try_drive_switch(len)) {
			bios_putch('?');
			bios_putch('\n');
		}
	}
	return 0; /* unreachable - a real shell never returns */
}
