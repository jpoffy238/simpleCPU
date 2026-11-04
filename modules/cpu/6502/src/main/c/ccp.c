/* A minimal CP/M-style CCP (Console Command Processor) - just the shell loop
 * and drive switching (A:, B:, ...), proven out before any filesystem code
 * exists on top of it. DIR and friends come later, once the directory-
 * scanning logic is ported from CpmFileSystem.java (modules/Disk) to C.
 *
 * Unlike hello.c, this never returns - a real shell takes over the machine
 * the way a booted DOS is meant to (B/the monitor is a sequential handoff,
 * not something a shell hands control back to after one line). */
#include "bios.h"

#define NUM_DRIVES 4
#define LINELEN 40

static unsigned char current_drive = 0;
static char line[LINELEN];

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
		if (!try_drive_switch(len)) {
			bios_putch('?');
			bios_putch('\n');
		}
	}
	return 0; /* unreachable - a real shell never returns */
}
