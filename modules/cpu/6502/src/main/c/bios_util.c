/* Small C-level conveniences built on top of BIOS.asm's primitives - nothing
 * here needs its own jump-table slot, since each is just a loop calling an
 * existing primitive (bios_putch()) repeatedly. See bios.h. */
#include "bios.h"

void bios_putstr(const char *s) {
	while (*s != '\0') {
		bios_putch(*s++);
	}
}
