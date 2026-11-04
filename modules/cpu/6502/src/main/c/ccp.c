/* A minimal CP/M-style CCP (Console Command Processor): the shell loop,
 * drive switching (A:, B:, ...), and now DIR - the first filesystem command,
 * ported from CpmFileSystem.java's directory-scanning logic (modules/Disk).
 *
 * Unlike hello.c, this never returns - a real shell takes over the machine
 * the way a booted DOS is meant to (B/the monitor is a sequential handoff,
 * not something a shell hands control back to after one line). */
#include "bios.h"

#define NUM_DRIVES 4
#define LINELEN 40

/* CP/M directory layout (see CpmFileSystem.java's class comment for the
 * full design) - a 1-byte status (STATUS_EMPTY = never used/deleted),
 * 11-byte name+extension, then extent/record-count/allocation. The whole
 * 64-entry directory lives in sectors 1-16 of track 1 (DIRECTORY_BLOCKS * 8
 * sectors/block = 16 sectors, 4 entries each = 64 bytes of directory per
 * sector / 32 bytes/entry = 4 entries/sector) - always on track 1, never
 * crossing into track 2, so unlike actual file data (which does span a
 * track boundary once a file needs more than one allocation block, i.e.
 * more than 1024 bytes - see CpmFileSystem's own test for exactly this)
 * this doesn't need a general block<->(track,sector) mapper yet. That's
 * deferred until TYPE (or whatever reads actual file content) needs it. */
#define DIR_TRACK 1
#define DIR_SECTORS 16
#define ENTRIES_PER_SECTOR 4
#define ENTRY_SIZE 32
#define STATUS_EMPTY 0xE5

static unsigned char current_drive = 0;
static char line[LINELEN];
static unsigned char sector_buf[128];

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

/* Reads directory sector `index` (0-15) into sector_buf. Always track 1 -
 * see the directory-layout comment above for why this doesn't need the
 * general mapper yet. */
static void read_directory_sector(unsigned char index) {
	bios_set_buffer(sector_buf);
	bios_set_x(index + 1); /* sector: directory sectors are 1-16 on track 1 */
	bios_read_sector(DIR_TRACK);
}

/* Prints one directory entry's name, space-padded trailing bytes trimmed,
 * as NAME or NAME.EXT. entry points at the start of a 32-byte FCB within
 * an already-read sector_buf. */
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

static void cmd_dir(void) {
	unsigned char sector_index, entry_index;
	unsigned char *entry;
	unsigned char found = 0;

	for (sector_index = 0; sector_index < DIR_SECTORS; sector_index++) {
		read_directory_sector(sector_index);
		for (entry_index = 0; entry_index < ENTRIES_PER_SECTOR; entry_index++) {
			entry = sector_buf + entry_index * ENTRY_SIZE;
			if (entry[0] != STATUS_EMPTY) {
				print_entry_name(entry);
				found = 1;
			}
		}
	}
	if (!found) {
		bios_putch('N');
		bios_putch('O');
		bios_putch(' ');
		bios_putch('F');
		bios_putch('I');
		bios_putch('L');
		bios_putch('E');
	}
	bios_putch('\n');
}

int main(void) {
	unsigned char len;

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
		} else if (!try_drive_switch(len)) {
			bios_putch('?');
			bios_putch('\n');
		}
	}
	return 0; /* unreachable - a real shell never returns */
}
