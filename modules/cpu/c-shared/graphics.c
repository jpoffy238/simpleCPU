/* Smoke test for the new graphics primitives (BIOS_GRAPHICS_MODE,
 * BIOS_SET_PIXEL - see BIOS.asm's header comment and VideoController's class
 * comment for the design). Draws an X spanning the whole 320x200 bitmap
 * (both diagonals, so a single off-by-one in BIOS_SET_PIXEL's row/column
 * math would show up as a visibly bent or clipped line rather than silently
 * drawing nothing), then switches back to text mode and prints "done" - a
 * working bios_putch() afterward confirms graphics mode didn't clobber the
 * character RAM or BIOS's own row/column tracking underneath it. */
#include "bios.h"

int main(void) {
	unsigned int x;
	unsigned int y;

	bios_graphics_mode(BIOS_MODE_GRAPHICS);

	for (x = 0; x < BIOS_GRAPHICS_COLS; x++) {
		y = (unsigned int) ((unsigned long) x * (BIOS_GRAPHICS_ROWS - 1) / (BIOS_GRAPHICS_COLS - 1));

		bios_set_x(1);
		bios_set_pixel_x(x);
		bios_set_pixel((unsigned char) y);

		bios_set_x(1);
		bios_set_pixel_x(x);
		bios_set_pixel((unsigned char) (BIOS_GRAPHICS_ROWS - 1 - y));
	}

	bios_graphics_mode(BIOS_MODE_TEXT);
	bios_putstr("done");
	return 0;
}
