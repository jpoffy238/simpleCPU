/* Smoke test for the cc65 toolchain setup (simplecpu.cfg + bios_glue.s) - the
 * exact same "Hello" the hand-written 6502 boot demo prints (see MONITOR.asm's
 * B command and Test_BIOS's bootedProgramCanPrintThroughTheBiosPutchPrimitive),
 * just through cc65/C instead of crasm/assembly. The point here is only to
 * confirm the compile-assemble-link pipeline produces a binary at all; running
 * it inside the emulator is a separate step for later (and today's B command
 * only loads a single 128-byte sector, which may or may not be enough once
 * cc65's startup overhead is counted - not this test's concern either way). */
#include "bios.h"

int main(void) {
	bios_putch('H');
	bios_putch('e');
	bios_putch('l');
	bios_putch('l');
	bios_putch('o');
	return 0;
}
