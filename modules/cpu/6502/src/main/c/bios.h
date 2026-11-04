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
char bios_getch(void);
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

#endif
