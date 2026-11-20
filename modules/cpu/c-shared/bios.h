/* C-facing declarations for the BIOS's console/disk/graphics primitives -
 * bridged via bios_glue.s (cc65/6502) or bios_glue.asm (SDCC/Z80), one of
 * each living under that CPU's own src/main/c since the two toolchains use
 * incompatible assembler syntax for an identical idea (JP/JMP straight to a
 * fixed jump-table address). This header, and the .c files that only call
 * through it, are shared between both CPUs unchanged - see
 * modules/cpu/c-shared's own place in the build for how each Makefile reaches
 * it.
 *
 * Multi-parameter primitives (read/write sector, format track) are split into
 * a "stage one value, then trigger" sequence rather than one function taking
 * every parameter at once: bios_set_buffer() for the pointer, bios_set_x()
 * for the second hardware register (sector number for read/write, fill byte
 * for format), then the track-taking call itself. cc65's calling convention
 * for more than one stack-passed C parameter needs correctly popping its own
 * software stack in the glue code - not worth the risk of getting wrong
 * without being able to compile/test it here. cc65's own __fastcall__ for a
 * single parameter (received directly in A, no software stack involved) is
 * simple and well-documented, so every function below takes at most one -
 * and SDCC's own default z80 calling convention (sdcccall(1)) already passes
 * a single byte/word argument in a register with no extra qualifier needed,
 * so BIOS_FASTCALL below is cc65-only, empty under SDCC. */
#ifndef BIOS_H
#define BIOS_H

#ifdef __CC65__
#define BIOS_FASTCALL __fastcall__
#else
#define BIOS_FASTCALL
#endif

void BIOS_FASTCALL bios_putch(char c);

/* Z80-only: one-time video setup (registers, screen clear, row/col reset) -
 * on the 6502 side this already happened in MONITOR.asm before any program
 * got booted, so cc65 code never needed to call it itself; the Z80 side has
 * no monitor yet (BIOS.z80 boots straight into a program), so any Z80 C
 * program that uses bios_putch() must call this itself first, same as
 * Demo.asm (see src/main/asm) does by hand. */
#ifdef __SDCC
void bios_video_init(void);
#endif

/* Blocks until a key is available, then returns it - an ordinary ASCII byte
 * for anything typed normally, or one of KEY_UP/DOWN/LEFT/RIGHT/PAGE_UP/
 * PAGE_DOWN below for a key with no character of its own
 * (VideoController.java's keyTyped() never fires for those, so they're
 * queued as synthetic codes outside plain ASCII's 0-127 range instead - see
 * VideoController.KEY_UP et al.). */
char bios_getch(void);

#define KEY_UP        0x80
#define KEY_DOWN      0x81
#define KEY_LEFT      0x82
#define KEY_RIGHT     0x83
#define KEY_PAGE_UP   0x84
#define KEY_PAGE_DOWN 0x85

/* Prints a null-terminated string one character at a time via bios_putch() -
 * no new BIOS primitive behind this, just the loop every caller was already
 * writing by hand (ccp.c's own print_string(), for one). Lives in
 * bios_util.c, not bios_glue.s, since it's ordinary C calling an existing
 * primitive repeatedly, not a new hardware operation needing its own
 * jump-table slot. */
void bios_putstr(const char *s);
void BIOS_FASTCALL bios_select(unsigned char drive);

/* Stages the sector-buffer pointer into ADDRLO/ADDRHI ahead of a read/write. */
void BIOS_FASTCALL bios_set_buffer(void *ptr);

/* Stages the second hardware register: sector number for bios_read_sector()/
 * bios_write_sector(), fill byte for bios_format_track(). */
void BIOS_FASTCALL bios_set_x(unsigned char value);

void BIOS_FASTCALL bios_read_sector(unsigned char track);
void BIOS_FASTCALL bios_write_sector(unsigned char track);
void BIOS_FASTCALL bios_format_track(unsigned char track);

/* Homes the selected drive's head to track 0. No parameters. */
void bios_restore(void);

/* Repoints bios_putch()'s next write (and the hardware cursor bar) at an
 * arbitrary (row, col) - the only way to reach a screen cell out of
 * sequence; bios_putch() on its own only ever advances. col is staged via
 * bios_set_x() first, same "stage one value, then trigger" pairing as
 * bios_read_sector()/bios_write_sector() above; row arrives directly in A.
 * No bounds checking - row must be < 24, col < 80 (BIOS.asm's V_ROWS/V_COLS)
 * or the result silently wraps into adjacent rows. */
void BIOS_FASTCALL bios_gotoxy(unsigned char row);

#define BIOS_SCREEN_COLS 80
#define BIOS_SCREEN_ROWS 24

/* Clears count columns starting at (row, col) to spaces, then leaves the
 * cursor parked back at (row, col) - conio's cclearxy(), built from
 * bios_gotoxy()'s positioning plus bios_putch()'s ordinary sequential
 * advance (no new BIOS primitive needed: printing count spaces after one
 * bios_gotoxy() call already lands them left-to-right from col). */
void bios_clear_eol(unsigned char row, unsigned char col, unsigned char count);

/* Clears the whole BIOS_SCREEN_ROWS x BIOS_SCREEN_COLS screen and parks the
 * cursor at (0, 0) - conio's clrscr(), same "just loop bios_clear_eol()"
 * construction. */
void bios_clrscr(void);

/* Switches VideoController's Mode Control register - BIOS_MODE_TEXT (the
 * power-on default) or BIOS_MODE_GRAPHICS. VideoController.getPixel() (and
 * its on-screen painting) only ever consults BIOS_GRAPHICS_RAM while in
 * graphics mode; bios_putch()/bios_gotoxy() keep working on the character
 * RAM underneath regardless, same as real CGA-class hardware kept its text
 * framebuffer intact while a graphics mode was selected. */
void BIOS_FASTCALL bios_graphics_mode(unsigned char on);

#define BIOS_MODE_TEXT     0
#define BIOS_MODE_GRAPHICS 1

#define BIOS_GRAPHICS_COLS 320
#define BIOS_GRAPHICS_ROWS 200

/* Stages the pixel's column (0-319) ahead of bios_set_pixel() - its own
 * function rather than reusing bios_set_buffer(), even though the staging
 * mechanics (ADDRLO/ADDRHI) are identical, since "pointer" and "pixel
 * column" are different things to a caller reading this header. */
void BIOS_FASTCALL bios_set_pixel_x(unsigned int x);

/* Sets or clears one pixel at (x, y) - x staged via bios_set_pixel_x() and
 * on/off staged via the existing bios_set_x() above, y arrives directly in
 * A on this trigger call (same "stage one value, then trigger" pairing as
 * bios_read_sector()/bios_gotoxy()). No bounds checking - x must be <
 * BIOS_GRAPHICS_COLS, y < BIOS_GRAPHICS_ROWS or the result silently wraps
 * into an adjacent row. */
void BIOS_FASTCALL bios_set_pixel(unsigned char y);

/* Copies RTC's raw Unix-epoch-seconds counter (see RTC.java) into *out,
 * converting it from the hardware's big-endian register layout into this
 * CPU's native little-endian unsigned long - *out is then an ordinary
 * seconds-since-the-epoch count, this project's own time_t (no calendar/
 * display conversion exists here, same deliberate omission as RTC.java's
 * own comment, and no <time.h> dependency on either toolchain). Takes the
 * pointer directly (one parameter - no "stage then trigger" split needed,
 * unlike bios_read_sector() etc.), same style as bios_set_buffer(). */
void BIOS_FASTCALL bios_get_time(unsigned long *out);

#endif
