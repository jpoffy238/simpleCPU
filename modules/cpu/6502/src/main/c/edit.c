/* edit.c - a minimal nano-style full-screen text editor.
 *
 * Ported from AdveditC64 (Dr. Eric O. Flores, MIT licensed), a Commodore 64
 * editor written against cc65's C64-specific conio.h (screen) and cbm.h
 * (IEC file I/O). Neither exists for this system, so both are replaced
 * entirely:
 *   - conio.h's gotoxy/cputs/cputc/clrscr/cclearxy -> bios_gotoxy()/
 *     bios_putch()/bios_clrscr()/bios_clear_eol() (bios.h) - built for this
 *     system's own VideoController hardware, not a C64's VIC-II.
 *   - cbm.h's cbm_open/cbm_read/cbm_write (IEC device numbers) -> this
 *     system's record-based BDOS calls (bdos_find_file/read_record/
 *     create_file/write_record - bdos.h), the same primitives CCP.COM's own
 *     TYPE/COPY/SAVE already use. There's no device-number concept here -
 *     just the one currently-selected drive (BIOS_SELECT).
 * The original also hand-drew its own reverse-video caret, since the C64
 * has no hardware cursor it can address directly - this system's
 * VideoController does (the same cursor bar MONITOR/CCP already rely on),
 * so bios_gotoxy() parks it directly instead; there's no caret-drawing code
 * here at all. Its 40-column soft-wrap (a workaround for the C64's narrow
 * display) is gone too - this system's real 80-column screen needs none of
 * that, so one logical line is always exactly one screen row.
 *
 * Minimal first pass: open/edit/save/quit, cursor movement, and line
 * insert/delete only - no menus, clipboard, or search yet (see the original
 * for what those looked like, if they're wanted here later). Returns
 * normally on exit, same as hello.c - BDOS's warm boot (bdos_run_file(),
 * ccp.c) reloads CCP.COM fresh at that point.
 */
#include "bios.h"
#include "bdos.h"

#define TITLE_Y  0
#define VIEW_Y   1
#define VIEW_H   22
#define STATUS_Y 23

#define MAX_LINES 100
#define MAX_COLS  79		/* one full screen row per logical line */
#define NAME_LEN  13		/* 8.3 name + '.' + null, one byte to spare */

#define CTRL_O 0x0F
#define CTRL_S 0x13
#define CTRL_X 0x18

/* How much of the viewport the next draw needs to touch - set by
 * handle_key() (never inside the editing ops themselves, so all of this
 * stays in one place), consumed and reset to DIRTY_NONE once per main loop
 * iteration. Redrawing all VIEW_H rows on literally every keystroke (what
 * the first pass did) is visibly flickery for something as common as a
 * single character insert or a pure cursor move, neither of which touches
 * more than one row - or, for a pure move, no row at all. */
#define DIRTY_NONE 0		/* only the cursor itself needs repositioning */
#define DIRTY_LINE 1		/* one row's content changed - redraw just it */
#define DIRTY_FULL 2		/* lines shifted (split/join) or a file loaded */

static unsigned char lines[MAX_LINES][MAX_COLS + 1];
static unsigned char line_count = 1;

static unsigned char cur_line = 0;
static unsigned char cur_col = 0;
static unsigned char view_top = 0;

static char cur_name[NAME_LEN];
static unsigned char has_name = 0;

/* Set by status_msg(), cleared by the next draw_status() - lets a just-set
 * message (e.g. "Saved") survive one full redraw-then-keypress-wait cycle
 * before the filename/position line replaces it, instead of being
 * overwritten the instant the main loop redraws (what the original did,
 * which made its own status_msg() calls effectively invisible). */
static unsigned char status_pending = 0;

static unsigned char dirty = DIRTY_FULL;	/* force the very first draw */

static unsigned char line_len(unsigned char i) {
	unsigned char n = 0;
	while (lines[i][n] != '\0') {
		n++;
	}
	return n;
}

/* strcpy-equivalent for the fixed-size line buffers - no <string.h> anywhere
 * in this codebase (see bios_util.c/ccp.c), so every string op here is a
 * plain hand-rolled loop, same convention. */
static void copy_line(unsigned char dest, unsigned char src) {
	unsigned char i = 0;
	do {
		lines[dest][i] = lines[src][i];
	} while (lines[src][i++] != '\0');
}

static void copy_name(char *dest, const char *src) {
	unsigned char i;
	for (i = 0; i < NAME_LEN - 1 && src[i] != '\0'; i++) {
		dest[i] = src[i];
	}
	dest[i] = '\0';
}

/* Appends s onto buf at len (bounded by BIOS_SCREEN_COLS), returning the
 * new length - used to build draw_status()'s whole line in one local
 * buffer before it ever touches the screen, so the single write-then-pad
 * pass below can pad exactly the real leftover tail instead of guessing. */
static unsigned char append_str(char *buf, unsigned char len, const char *s) {
	while (*s != '\0' && len < BIOS_SCREEN_COLS) {
		buf[len++] = *s++;
	}
	return len;
}

static unsigned char append_decimal(char *buf, unsigned char len, unsigned int value) {
	char digits[5];
	unsigned char count = 0;

	if (value == 0) {
		if (len < BIOS_SCREEN_COLS) {
			buf[len++] = '0';
		}
		return len;
	}
	while (value > 0) {
		digits[count++] = '0' + (value % 10);
		value /= 10;
	}
	while (count > 0 && len < BIOS_SCREEN_COLS) {
		buf[len++] = digits[--count];
	}
	return len;
}

/* Writes text (len bytes) starting at (row, 0), then pads whatever's left
 * of the row with spaces - same visual result as clearing the row first
 * and writing into it, but without ever blanking the text itself: clearing
 * first means there's a real window, however brief, where the row holds
 * nothing but spaces - and VideoController's periodic repaint (not driven
 * by individual writes) can land exactly there and show it as a one-frame
 * blank flash. Writing the real content first and only padding the
 * (usually already-blank) leftover tail keeps that window off the part
 * actually being looked at. */
static void draw_text_row(unsigned char row, const char *text, unsigned char len) {
	unsigned char i;
	unsigned char limit = BIOS_SCREEN_COLS;

	/* Same one-column-short cap as bios_clear_eol() (bios_util.c) and for
	 * the same reason: filling the screen's absolute last cell through
	 * bios_putch() triggers DO_PUTCH's auto-wrap scroll, which this
	 * STATUS_Y row (the only one that's also BIOS_SCREEN_ROWS-1) hits on
	 * every single redraw otherwise - scrolling the whole screen on what
	 * was meant to be a plain status-line update. */
	if (row == BIOS_SCREEN_ROWS - 1) {
		limit = BIOS_SCREEN_COLS - 1;
	}

	bios_set_x(0);
	bios_gotoxy(row);
	for (i = 0; i < len && i < limit; i++) {
		bios_putch(text[i]);
	}
	for (; i < limit; i++) {
		bios_putch(' ');
	}
}

static void status_msg(const char *msg) {
	unsigned char len = 0;

	while (msg[len] != '\0' && len < BIOS_SCREEN_COLS) {
		len++;
	}
	draw_text_row(STATUS_Y, msg, len);
	status_pending = 1;
}

static void draw_status(void) {
	char buf[BIOS_SCREEN_COLS];
	unsigned char len = 0;
	const char *name;
	unsigned char i;

	if (status_pending) {
		status_pending = 0;
		return;
	}

	name = has_name ? cur_name : "Untitled";
	for (i = 0; name[i] != '\0' && len < BIOS_SCREEN_COLS; i++) {
		buf[len++] = name[i];
	}
	len = append_str(buf, len, "  L:");
	len = append_decimal(buf, len, cur_line + 1);
	len = append_str(buf, len, " C:");
	len = append_decimal(buf, len, cur_col + 1);
	draw_text_row(STATUS_Y, buf, len);
}

/* Redraws one viewport row (screen_row, 0-based within the viewport) with
 * logical line l's content, or blank if l is past the end of the buffer. */
static void draw_row(unsigned char screen_row, unsigned char l) {
	unsigned char n = (l < line_count) ? line_len(l) : 0;

	draw_text_row(VIEW_Y + screen_row, (const char *) lines[l], n);
}

/* Parks the hardware cursor at the screen position matching cur_line/
 * cur_col - scroll_to_caret() having already run guarantees cur_line falls
 * within [view_top, view_top + VIEW_H), so this never needs bounds-checking
 * of its own. Doesn't touch row content at all - the common case for a
 * plain cursor move with nothing else changed. */
static void position_cursor(void) {
	bios_set_x(cur_col);
	bios_gotoxy(VIEW_Y + (cur_line - view_top));
}

static void draw_view(void) {
	unsigned char row;

	for (row = 0; row < VIEW_H; row++) {
		draw_row(row, view_top + row);
	}
	position_cursor();
}

static void draw_current_row(void) {
	draw_row(cur_line - view_top, cur_line);
	position_cursor();
}

/* Keeps the cursor's logical line within the viewport by sliding view_top -
 * no wrap segments to account for (unlike the original), since one logical
 * line is always exactly one screen row here. */
static void scroll_to_caret(void) {
	if (cur_line < view_top) {
		view_top = cur_line;
	} else if (cur_line >= view_top + VIEW_H) {
		view_top = cur_line - VIEW_H + 1;
	}
}

static void clamp_col(void) {
	unsigned char len = line_len(cur_line);
	if (cur_col > len) {
		cur_col = len;
	}
}

static void move_left(void) {
	if (cur_col > 0) {
		cur_col--;
	} else if (cur_line > 0) {
		cur_line--;
		cur_col = line_len(cur_line);
	}
}

static void move_right(void) {
	unsigned char len = line_len(cur_line);
	if (cur_col < len) {
		cur_col++;
	} else if (cur_line + 1 < line_count) {
		cur_line++;
		cur_col = 0;
	}
}

static void move_up(void) {
	if (cur_line > 0) {
		cur_line--;
		clamp_col();
	}
}

static void move_down(void) {
	if (cur_line + 1 < line_count) {
		cur_line++;
		clamp_col();
	}
}

static void insert_char(unsigned char ch) {
	unsigned char len = line_len(cur_line);
	unsigned char i;

	if (len >= MAX_COLS) {
		return;
	}
	for (i = len + 1; i > cur_col; i--) {
		lines[cur_line][i] = lines[cur_line][i - 1];
	}
	lines[cur_line][cur_col] = ch;
	cur_col++;
}

/* Returns 1 if a cross-line join happened - every line below the join point
 * shifts up, so the caller needs a full viewport redraw, not just the one
 * row a same-line delete (0) would need. */
static unsigned char backspace_char(void) {
	unsigned char len = line_len(cur_line);
	unsigned char i, prev_len;

	if (cur_col == 0) {
		if (cur_line == 0) {
			return 0;
		}
		prev_len = line_len(cur_line - 1);
		if (prev_len + len > MAX_COLS) {
			return 0;
		}
		for (i = 0; i <= len; i++) {
			lines[cur_line - 1][prev_len + i] = lines[cur_line][i];
		}
		for (i = cur_line; i + 1 < line_count; i++) {
			copy_line(i, i + 1);
		}
		line_count--;
		cur_line--;
		cur_col = prev_len;
		return 1;
	}

	for (i = cur_col - 1; i < len; i++) {
		lines[cur_line][i] = lines[cur_line][i + 1];
	}
	cur_col--;
	return 0;
}

static void split_line(void) {
	unsigned char len = line_len(cur_line);
	unsigned char i;

	if (line_count >= MAX_LINES) {
		return;
	}
	for (i = line_count; i > cur_line + 1; i--) {
		copy_line(i, i - 1);
	}
	line_count++;
	for (i = cur_col; i <= len; i++) {
		lines[cur_line + 1][i - cur_col] = lines[cur_line][i];
	}
	lines[cur_line][cur_col] = '\0';
	cur_line++;
	cur_col = 0;
}

/* Prompt at the status line, same shape as the original's prompt_input() -
 * ESC cancels (returns 0, *out untouched beyond out[0]=0), Enter accepts.
 * Backspace erases both the buffer and its on-screen echo. */
static unsigned char prompt_input(const char *prompt, char *out, unsigned char outmax) {
	unsigned char n = 0;
	unsigned char ch;
	unsigned char plen = 0;
	const char *p;

	for (p = prompt; *p != '\0'; p++) {
		plen++;
	}

	bios_clear_eol(STATUS_Y, 0, BIOS_SCREEN_COLS);
	bios_putstr(prompt);
	out[0] = '\0';

	for (;;) {
		ch = bios_getch();
		if (ch == '\r' || ch == '\n') {
			break;
		}
		if (ch == 27) {
			out[0] = '\0';
			return 0;
		}
		if (ch == '\b' || ch == 0x7F) {
			if (n > 0) {
				n--;
				out[n] = '\0';
				bios_set_x(plen + n);
				bios_gotoxy(STATUS_Y);
				bios_putch(' ');
				bios_set_x(plen + n);
				bios_gotoxy(STATUS_Y);
			}
			continue;
		}
		if (ch >= ' ' && ch < 0x80 && n + 1 < outmax) {
			out[n++] = ch;
			out[n] = '\0';
			bios_putch(ch);
		}
	}
	return n > 0;
}

/* Resets the buffer, then fills it record-by-record from the file already
 * found by the caller's bdos_find_file() - same BDOS primitives cmd_type()
 * (ccp.c) reads through, just split into lines on CR instead of printed
 * straight out. A record's unused tail (past the file's real end) reads
 * back as whatever padding was written there, usually '\0' - skipped here
 * same as it would need to be regardless of which reader hit it first. */
static void load_file(void) {
	unsigned char record[128];
	unsigned char l, col, i, ch;

	for (l = 0; l < MAX_LINES; l++) {
		lines[l][0] = '\0';
	}
	cur_line = 0;
	cur_col = 0;
	view_top = 0;

	l = 0;
	col = 0;
	for (;;) {
		bios_set_buffer(record);
		if (!bdos_read_record()) {
			break;
		}
		for (i = 0; i < 128; i++) {
			ch = record[i];
			if (ch == '\r') {
				if (l + 1 < MAX_LINES) {
					lines[l][col] = '\0';
					l++;
					col = 0;
				}
				continue;
			}
			if (ch == '\n' || ch == '\0') {
				continue;
			}
			if (col < MAX_COLS) {
				lines[l][col++] = ch;
				lines[l][col] = '\0';
			}
		}
	}
	lines[l][col] = '\0';
	line_count = l + 1;
}

static void cmd_open(void) {
	char name[NAME_LEN];

	if (!prompt_input("Open: ", name, NAME_LEN)) {
		status_msg("");
		return;
	}
	bios_set_buffer(name);
	if (!bdos_find_file()) {
		status_msg("Not found");
		return;
	}
	load_file();
	copy_name(cur_name, name);
	has_name = 1;
	status_msg("Loaded");
	dirty = DIRTY_FULL;
}

/* Writes every line back out as 128-byte BDOS records, CR-terminated per
 * line (matching load_file()'s own split-on-CR convention) - deletes any
 * existing file under the same name first (bdos_create_file() alone fails
 * if the name already exists; ignoring delete's result is fine, since 0
 * just means there was nothing to delete yet). */
static void save_file(void) {
	unsigned char record[128];
	unsigned char l, i, col, len;

	bios_set_buffer(cur_name);
	bdos_delete_file();
	bios_set_buffer(cur_name);
	if (!bdos_create_file()) {
		status_msg("Cannot create");
		return;
	}

	col = 0;
	for (l = 0; l < line_count; l++) {
		len = line_len(l);
		for (i = 0; i < len; i++) {
			record[col++] = lines[l][i];
			if (col == 128) {
				bios_set_buffer(record);
				if (!bdos_write_record()) {
					status_msg("Disk full");
					return;
				}
				col = 0;
			}
		}
		record[col++] = '\r';
		if (col == 128) {
			bios_set_buffer(record);
			if (!bdos_write_record()) {
				status_msg("Disk full");
				return;
			}
			col = 0;
		}
	}
	if (col > 0) {
		for (i = col; i < 128; i++) {
			record[i] = '\0';
		}
		bios_set_buffer(record);
		if (!bdos_write_record()) {
			status_msg("Disk full");
			return;
		}
	}
	status_msg("Saved");
}

static void cmd_save(void) {
	char name[NAME_LEN];

	if (!has_name) {
		if (!prompt_input("Save as: ", name, NAME_LEN)) {
			status_msg("");
			return;
		}
		copy_name(cur_name, name);
		has_name = 1;
	}
	save_file();
}

static void handle_key(unsigned char k, unsigned char *quit_flag) {
	if (k == CTRL_X) {
		*quit_flag = 1;
		return;
	}
	if (k == CTRL_O) {
		cmd_open();
		return;
	}
	if (k == CTRL_S) {
		cmd_save();
		return;
	}
	if (k == KEY_UP) {
		move_up();
		return;
	}
	if (k == KEY_DOWN) {
		move_down();
		return;
	}
	if (k == KEY_LEFT) {
		move_left();
		return;
	}
	if (k == KEY_RIGHT) {
		move_right();
		return;
	}
	if (k == '\r' || k == '\n') {
		split_line();
		dirty = DIRTY_FULL;
		return;
	}
	if (k == '\b' || k == 0x7F) {
		dirty = backspace_char() ? DIRTY_FULL : DIRTY_LINE;
		return;
	}
	if (k >= ' ' && k < 0x80) {
		insert_char(k);
		dirty = DIRTY_LINE;
	}
}

int main(void) {
	unsigned char quit_flag = 0;
	unsigned char k;
	unsigned char old_top;

	lines[0][0] = '\0';

	bios_clrscr();
	bios_set_x(0);
	bios_gotoxy(TITLE_Y);
	bios_putstr("^O Open  ^S Save  ^X Exit");

	while (!quit_flag) {
		old_top = view_top;
		scroll_to_caret();
		if (view_top != old_top) {
			dirty = DIRTY_FULL;
		}

		if (dirty == DIRTY_FULL) {
			draw_view();
		} else if (dirty == DIRTY_LINE) {
			draw_current_row();
		} else {
			position_cursor();
		}
		dirty = DIRTY_NONE;

		draw_status();
		k = bios_getch();
		handle_key(k, &quit_flag);
	}

	bios_clrscr();
	return 0;
}
