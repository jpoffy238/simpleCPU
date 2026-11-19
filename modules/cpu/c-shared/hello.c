/* Smoke test for the C toolchain setup on either CPU - cc65 (simplecpu.cfg +
 * bios_glue.s) on the 6502 side, SDCC (bios_glue.asm) on the Z80 side - the
 * exact same "Hello" the hand-written boot demo on each CPU prints (6502:
 * MONITOR.asm's B command and Test_BIOS's
 * bootedProgramCanPrintThroughTheBiosPutchPrimitive; Z80: Demo.asm and
 * Test_Z80Boot), just through C instead of hand-written assembly. The point
 * here is only to confirm the compile-assemble-link pipeline produces a
 * binary at all - shared unchanged between both CPUs via bios.h's one real
 * difference (bios_video_init(), Z80-only - see bios.h). */
#include "bios.h"

int main(void) {
#ifdef __SDCC
	bios_video_init();
#endif
	bios_putch('H');
	bios_putch('e');
	bios_putch('l');
	bios_putch('l');
	bios_putch('o');
	return 0;
}
