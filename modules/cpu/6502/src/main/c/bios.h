/* C-facing declarations for BIOS.asm's primitives, bridged via bios_glue.s.
 *
 * Multi-parameter primitives (read/write sector, format track) are split into
 * a "stage one value, then trigger" sequence rather than one function taking
 * every parameter at once: bios_set_buffer() for the pointer, bios_set_x()
 * for the second hardware register (sector number for read/write, fill byte
 * for format), then the track-taking call itself. cc65's calling convention
 * for more than one stack-passed C parameter needs correctly popping its own
 * software stack in the glue code - not worth the risk of getting wrong
 * without being able to compile/test it here. __fastcall__ for a single
 * parameter (received directly in A, no software stack involved) is simple
 * and well-documented, so every function below takes at most one. */
#ifndef BIOS_H
#define BIOS_H

void __fastcall__ bios_putch(char c);

/* Blocks until a key is available, then returns it - an ordinary ASCII byte
 * for anything typed normally, or one of KEY_UP/DOWN/LEFT/RIGHT below for an
 * arrow key (VideoController.java's keyTyped() never produces a character
 * for those, so they're queued as synthetic codes outside plain ASCII's
 * 0-127 range instead - see VideoController.KEY_UP et al.). */
char bios_getch(void);

#define KEY_UP    0x80
#define KEY_DOWN  0x81
#define KEY_LEFT  0x82
#define KEY_RIGHT 0x83

/* Prints a null-terminated string one character at a time via bios_putch() -
 * no new BIOS primitive behind this, just the loop every caller was already
 * writing by hand (ccp.c's own print_string(), for one). Lives in
 * bios_util.c, not bios_glue.s, since it's ordinary C calling an existing
 * primitive repeatedly, not a new hardware operation needing its own
 * jump-table slot. */
void bios_putstr(const char *s);
void __fastcall__ bios_select(unsigned char drive);

/* Stages the sector-buffer pointer into ADDRLO/ADDRHI ahead of a read/write. */
void __fastcall__ bios_set_buffer(void *ptr);

/* Stages the second hardware register: sector number for bios_read_sector()/
 * bios_write_sector(), fill byte for bios_format_track(). */
void __fastcall__ bios_set_x(unsigned char value);

void __fastcall__ bios_read_sector(unsigned char track);
void __fastcall__ bios_write_sector(unsigned char track);
void __fastcall__ bios_format_track(unsigned char track);

/* Homes the selected drive's head to track 0. No parameters. */
void bios_restore(void);

/* Repoints bios_putch()'s next write (and the hardware cursor bar) at an
 * arbitrary (row, col) - the only way to reach a screen cell out of
 * sequence; bios_putch() on its own only ever advances. col is staged via
 * bios_set_x() first, same "stage one value, then trigger" pairing as
 * bios_read_sector()/bios_write_sector() above; row arrives directly in A.
 * No bounds checking - row must be < 24, col < 80 (BIOS.asm's V_ROWS/V_COLS)
 * or the result silently wraps into adjacent rows. */
void __fastcall__ bios_gotoxy(unsigned char row);

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

#endif
