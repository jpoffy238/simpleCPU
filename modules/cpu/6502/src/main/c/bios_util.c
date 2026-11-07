/* Small C-level conveniences built on top of BIOS.asm's primitives - nothing
 * here needs its own jump-table slot, since each is just a loop calling an
 * existing primitive (bios_putch()) repeatedly. See bios.h. */
#include "bios.h"

void bios_putstr(const char *s) {
	while (*s != '\0') {
		bios_putch(*s++);
	}
}

void bios_clear_eol(unsigned char row, unsigned char col, unsigned char count) {
	unsigned char i;
	unsigned char limit = col + count;

	/* Never let this write the screen's absolute last cell (row
	 * BIOS_SCREEN_ROWS-1, col BIOS_SCREEN_COLS-1) through bios_putch():
	 * DO_PUTCH's auto-wrap (BIOS.asm) scrolls the whole screen the
	 * instant that cell is filled - correct behavior for its original
	 * sequential-output purpose (a real terminal does the same thing at
	 * its bottom-right corner with autowrap on), but not what a
	 * positional clear like this one wants. Stopping one column short on
	 * that one row only means that single corner cell is simply never
	 * touched by a clear - a trade worth making to never trigger an
	 * unwanted scroll on an ordinary redraw. */
	if (row == BIOS_SCREEN_ROWS - 1 && limit > BIOS_SCREEN_COLS - 1) {
		limit = BIOS_SCREEN_COLS - 1;
	}

	bios_set_x(col);
	bios_gotoxy(row);
	for (i = col; i < limit; i++) {
		bios_putch(' ');
	}
	bios_set_x(col);
	bios_gotoxy(row);
}

void bios_clrscr(void) {
	unsigned char row;

	for (row = 0; row < BIOS_SCREEN_ROWS; row++) {
		bios_clear_eol(row, 0, BIOS_SCREEN_COLS);
	}
	bios_set_x(0);
	bios_gotoxy(0);
}
