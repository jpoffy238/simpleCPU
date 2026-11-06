;; Primitives ROM: raw hardware access only - console I/O and disk I/O - no
;; command parsing or filesystem concepts. That's MONITOR.asm's job; it calls
;; these services exclusively through the fixed jump table below, never
;; touching VIDEO_DATA/FD_STATUS/etc. itself. This file and MONITOR.asm are
;; assembled separately (two distinct ROM chips, see CPUMain/Test_BIOS), so the
;; jump table's addresses are a frozen ABI duplicated as constants over there -
;; keep both in sync if anything here ever moves.
;;
;; Calling convention:
;;   BIOS_PUTCH         A = char to print
;;   BIOS_GETCH         blocks until input available; returns A = char
;;   BIOS_SELECT        A = drive 0-3
;;   BIOS_READ_SECTOR   A = track, X = sector, zero-page ADDRLO/ADDRHI = dest
;;   BIOS_WRITE_SECTOR  A = track, X = sector, zero-page ADDRLO/ADDRHI = src
;;   BIOS_FORMAT_TRACK  A = track, X = fill byte (one track only - a caller
;;                      wanting the whole disk formatted loops over tracks)
;;   BIOS_RESTORE       no parameters - homes the selected drive's head back
;;                      to track 0, the WD179x's canonical "known state" command
;;   BIOS_VIDEO_INIT    no parameters - one-time setup: configures VideoController's
;;                      H/V Displayed and cursor, clears its character RAM, and resets
;;                      this file's own row/column tracking (see DO_PUTCH)
CPU 6502
OUTPUT HEX

;; FloppyController registers (see FloppyController.java)
FD_STATUS = $9010		;; status (read) / command (write)
FD_TRACK  = $9011
FD_SECTOR = $9012
FD_DATA   = $9013
FD_DRIVE  = $9014

;; VideoController registers (see VideoController.java) - Address/Data select
;; which of its 18 CRTC registers VIDEO_DATA refers to, same two-step dance BIOS
;; itself exposes no abstraction for (DO_PUTCH/DO_VIDEO_INIT poke these directly,
;; unlike the floppy registers, since there's no higher-level "BIOS_VIDEO_xxx"
;; primitive for individual register access - only the one-time init below).
VIDEO_ADDR = $9020
VIDEO_DATA = $9021
VR_H_DISPLAYED = 1
VR_V_DISPLAYED = 6
VR_CURSOR_START = 10
VR_CURSOR_ADDR_HI = 14
VR_CURSOR_ADDR_LO = 15
CURSOR_ON = $00			;; anything other than CURSOR_MODE_OFF below
CURSOR_MODE_OFF = $20			;; matches VideoController.CURSOR_MODE_OFF

;; VideoController's bolted-on keyboard interface (+2/+3 of the same
;; AddressRange as VIDEO_ADDR/VIDEO_DATA above - see its class comment for why
;; a CRTC register pair and a keyboard data/status pair end up sharing one
;; device). DATA_AVAILABLE is the one status bit DO_GETCH polls, same meaning
;; (and same bit) ConsoleDevice's own status register used.
VIDEO_KBD_DATA = $9022
VIDEO_KBD_STAT = $9023
DATA_AVAILABLE = $01

;; VideoController's character RAM (plain memory - BIOS pokes it directly, same
;; as any ordinary RAM access, no register protocol involved) and the fixed
;; 80x24 layout DO_VIDEO_INIT configures it for. V_COLS*V_ROWS (1920) is the
;; brute-force scroll's working size - deliberately not using the full 2KB
;; VideoController's char RAM device actually has, since Display Start Address
;; scrolling (using the rest of that space to scroll without copying) is a later
;; follow-up, not this pass.
VIDEO_RAM = $B800
V_COLS = 80
V_ROWS = 24

;; zero page: shared with MONITOR.asm - the sector-buffer address parameter for
;; BIOS_READ_SECTOR/BIOS_WRITE_SECTOR, and (only inside PC_SCROLL, saved and
;; restored there - see its comment) MONITOR's own BYTEVAL/TRACKVAL command
;; scratch, borrowed rather than claiming yet more zero page of this ROM's own.
;; Part of the frozen calling convention.
ADDRLO   = $01
ADDRHI   = $02
BYTEVAL  = $03
TRACKVAL = $04

;; zero page: BIOS's own row/column tracking for DO_PUTCH's video output -
;; VideoController's hardware has no concept of "where the next character goes"
;; (that's always been firmware's job, even on real CRTC-based machines), so
;; this file owns it. V_ADDR_LO/HI together are the address of the next
;; character's slot in VIDEO_RAM (0..1919 past VIDEO_RAM, never computed via
;; row*80+col multiplication - advancing it by 1 after column 79 already lands
;; exactly on the next row, since rows are contiguous); V_COL is tracked
;; alongside purely for CR/backspace/auto-wrap decisions. SAVED_CHAR/SAVED_Y
;; are DO_PUTCH's own save slots for the character and Y across its whole call
;; (see its comment for why) - never touched by PC_SCROLL, so they survive
;; even if a call happens to trigger a scroll.
;;
;; $09-$0D, NOT $05-$08 as an earlier pass of this file used - MONITOR.asm's
;; own EXEC_JSR/EXEC_LO/EXEC_HI/EXEC_RTS (its self-modifying-JSR scratch for
;; the E command) already live at $05-$08, and BIOS_PUTCH is called while
;; those are live (echoing every character of the *next* line typed after an
;; E command runs) - V_ADDR_LO/HI/V_COL need to survive from one BIOS_PUTCH
;; call to the next, so colliding there silently corrupted the cursor
;; position the moment E ran. $09-$0D are reserved for exactly this
;; (simplecpu.cfg/bdos.cfg's ZEROPAGE segment now starts at $0E instead of
;; $09 to make room) rather than overlapping cc65's own zero page (sp,
;; ptr1-4, etc. from $0A) the original $05-$09 choice also happened to do.
V_ADDR_LO  = $09
V_ADDR_HI  = $0A
V_COL      = $0B
SAVED_CHAR = $0C
SAVED_Y    = $0D

* = $F000

;; ---- jump table: fixed, frozen entry points. MONITOR.asm calls these by
;;      address; moving an implementation below is always safe as long as its
;;      slot here still JMPs to the right place. ----
	JMP DO_PUTCH		;; $F000 BIOS_PUTCH
	JMP DO_GETCH		;; $F003 BIOS_GETCH
	JMP DO_SELECT		;; $F006 BIOS_SELECT
	JMP DO_READ_SECTOR	;; $F009 BIOS_READ_SECTOR
	JMP DO_WRITE_SECTOR	;; $F00C BIOS_WRITE_SECTOR
	JMP DO_FORMAT_TRACK	;; $F00F BIOS_FORMAT_TRACK
	JMP DO_RESTORE		;; $F012 BIOS_RESTORE
	JMP DO_VIDEO_INIT	;; $F015 BIOS_VIDEO_INIT

;; ---- A = char to print. Pokes VideoController's character RAM with BIOS's
;;      own row/column tracking - Console drove this exclusively once, then
;;      both in parallel while Video's output was being validated against
;;      Console's known-good behavior; this is the cutover to Video being the
;;      only output, now that it's trusted.
;;
;;      The very first version of this routine (before Video existed at all)
;;      never touched X or Y and always left A holding the printed character,
;;      so callers still rely on all three surviving a BIOS_PUTCH call:
;;      READLINE does STA INBUF,Y / JSR BIOS_PUTCH / INY; CMD_DUMP keeps its
;;      16-line row counter in X across many JSR BIOS_PUTCH calls per line;
;;      and CMD_DUMP's own column-gap spacing does LDA #' ' / JSR BIOS_PUTCH /
;;      JSR BIOS_PUTCH, printing the same space twice by relying on A still
;;      holding it after the first call. X is never touched anywhere in this
;;      call chain (PC_SCROLL below uses address comparison instead of a row
;;      counter specifically to keep it that way), so it needs no saving; Y
;;      and the character are stashed in SAVED_Y/SAVED_CHAR via plain STY/STA
;;      (neither touches A, unlike the TYA/TXA a PHA-based save would need)
;;      and restored at PC_DONE. ----
DO_PUTCH STA SAVED_CHAR
	STY SAVED_Y
	CMP #$0D		;; CR: back to column 0, same row
	BEQ PC_CR
	CMP #$0A		;; LF: next row, column 0 (scrolling if needed)
	BEQ PC_LF
	CMP #$08		;; BS: one column left, no wrap (matches ConsoleDevice)
	BEQ PC_BS
	LDY #0			;; anything else: poke it at the current position, advance
	STA (V_ADDR_LO),Y
	JSR PC_ADVANCE
	JMP PC_DONE
PC_CR	LDA V_ADDR_LO
	SEC
	SBC V_COL
	STA V_ADDR_LO
	LDA V_ADDR_HI
	SBC #0
	STA V_ADDR_HI
	LDA #0
	STA V_COL
	JMP PC_DONE
PC_BS	LDA V_COL
	BEQ PC_DONE		;; already at column 0 - nothing to do
	DEC V_COL
	LDA V_ADDR_LO
	BNE PC_BS_SKIP
	DEC V_ADDR_HI
PC_BS_SKIP DEC V_ADDR_LO
	JMP PC_DONE
PC_LF	LDA #V_COLS
	SEC
	SBC V_COL		;; A = distance from here to the start of the next row
	CLC
	ADC V_ADDR_LO
	STA V_ADDR_LO
	LDA V_ADDR_HI
	ADC #0
	STA V_ADDR_HI
	LDA #0
	STA V_COL
	JSR PC_CHECK_SCROLL
	;; falls through to PC_DONE

;; ---- shared tail for every DO_PUTCH path: stage the Cursor Address register
;;      from the (possibly just-updated) V_ADDR_LO/HI, then restore the
;;      caller's Y and the original character into A (SAVED_Y/SAVED_CHAR,
;;      stashed at DO_PUTCH's top) before returning - the one thing every
;;      path above needs done exactly once, regardless of which branch got
;;      there. ----
PC_DONE	JSR PC_SET_CURSOR
	LDY SAVED_Y
	LDA SAVED_CHAR
	RTS

;; ---- stages the Cursor Address register (R14/R15) from V_ADDR_LO/HI, the
;;      offset from VIDEO_RAM rather than the absolute address (same "RAM
;;      offset, not a row/col" convention VideoController's own Cursor Address
;;      doc already uses). Self-contained (its one PHA/PLA pair is balanced
;;      internally) so DO_VIDEO_INIT can also JSR straight into it without
;;      first pushing anything of its own. ----
PC_SET_CURSOR LDA V_ADDR_LO
	SEC
	SBC #(VIDEO_RAM & $FF)
	TAY
	LDA V_ADDR_HI
	SBC #(VIDEO_RAM >> 8)
	PHA
	LDA #VR_CURSOR_ADDR_LO
	STA VIDEO_ADDR
	STY VIDEO_DATA
	LDA #VR_CURSOR_ADDR_HI
	STA VIDEO_ADDR
	PLA
	STA VIDEO_DATA
	RTS

;; ---- advances V_ADDR_LO/HI and V_COL by one position, wrapping the column
;;      at V_COLS (which needs no address adjustment of its own - row N+1
;;      starts immediately after row N's last byte in VIDEO_RAM, so a plain
;;      +1 already lands there) and scrolling once the screen's bottom is
;;      passed. ----
PC_ADVANCE INC V_COL
	LDA V_COL
	CMP #V_COLS
	BNE PC_ADV_NOWRAP
	LDA #0
	STA V_COL
PC_ADV_NOWRAP INC V_ADDR_LO
	BNE PC_CHECK_SCROLL
	INC V_ADDR_HI
	;; falls through to PC_CHECK_SCROLL

;; ---- scrolls once V_ADDR_LO/HI has advanced past the last visible byte
;;      (VIDEO_RAM + V_COLS*V_ROWS = $B800+1920 = $BF80). Only ever reached
;;      right after a row boundary (auto-wrap or explicit LF both land here
;;      with V_COL already 0), so there's nothing left to do beyond the
;;      scroll itself. ----
PC_CHECK_SCROLL LDA V_ADDR_LO
	CMP #((VIDEO_RAM+V_COLS*V_ROWS) & $FF)
	BNE PC_CS_DONE
	LDA V_ADDR_HI
	CMP #((VIDEO_RAM+V_COLS*V_ROWS) >> 8)
	BNE PC_CS_DONE
	JSR PC_SCROLL
PC_CS_DONE RTS

;; ---- brute-force scroll: copies rows 1-23 up into rows 0-22 one row at a
;;      time, blanks the newly-vacated last row, and repositions V_ADDR_LO/HI
;;      there with V_COL left at 0 (already true on every caller's path into
;;      here).
;;
;;      Borrows ADDRLO/ADDRHI (source pointer) and BYTEVAL/TRACKVAL
;;      (destination pointer) rather than claiming more permanent zero page -
;;      all four are saved on the hardware stack at entry and restored at
;;      exit, since (unlike ADDRLO/ADDRHI's already-established "clobbered
;;      freely across BDOS calls" convention) BYTEVAL/TRACKVAL are MONITOR's
;;      own command-argument scratch, and this runs from an ordinary
;;      BIOS_PUTCH call - not a dedicated primitive a command handler already
;;      expects to lose state across.
;;
;;      Loop termination compares the destination pointer against the last
;;      row's start address instead of counting rows in X: X has no "safe to
;;      clobber" precedent here the way ADDRLO/ADDRHI do - CMD_DUMP keeps its
;;      own row counter in X across many BIOS_PUTCH calls, so nothing in
;;      DO_PUTCH's whole call chain touches X at all. ----
PC_SCROLL LDA ADDRLO
	PHA
	LDA ADDRHI
	PHA
	LDA BYTEVAL
	PHA
	LDA TRACKVAL
	PHA
	LDA #((VIDEO_RAM+V_COLS) & $FF)
	STA ADDRLO
	LDA #((VIDEO_RAM+V_COLS) >> 8)
	STA ADDRHI
	LDA #(VIDEO_RAM & $FF)
	STA BYTEVAL
	LDA #(VIDEO_RAM >> 8)
	STA TRACKVAL
PS_ROW_LOOP LDY #0
PS_BYTE_LOOP LDA (ADDRLO),Y
	STA (BYTEVAL),Y
	INY
	CPY #V_COLS
	BNE PS_BYTE_LOOP
	LDA ADDRLO
	CLC
	ADC #V_COLS
	STA ADDRLO
	LDA ADDRHI
	ADC #0
	STA ADDRHI
	LDA BYTEVAL
	CLC
	ADC #V_COLS
	STA BYTEVAL
	LDA TRACKVAL
	ADC #0
	STA TRACKVAL
	CMP #((VIDEO_RAM+(V_ROWS-1)*V_COLS) >> 8)
	BNE PS_ROW_LOOP
	LDA BYTEVAL
	CMP #((VIDEO_RAM+(V_ROWS-1)*V_COLS) & $FF)
	BNE PS_ROW_LOOP
	LDY #0
PS_BLANK_LOOP LDA #' '
	STA (BYTEVAL),Y
	INY
	CPY #V_COLS
	BNE PS_BLANK_LOOP
	LDA BYTEVAL
	STA V_ADDR_LO
	LDA TRACKVAL
	STA V_ADDR_HI
	PLA
	STA TRACKVAL
	PLA
	STA BYTEVAL
	PLA
	STA ADDRHI
	PLA
	STA ADDRLO
	RTS

;; ---- one-time setup, called once from MONITOR.asm's boot path (see
;;      RESET_INIT) - configures the registers DO_PUTCH's video output
;;      actually relies on, clears the visible screen (otherwise it'd show
;;      whatever garbage happens to already be in VIDEO_RAM), and resets this
;;      file's own row/column tracking to match. ----
DO_VIDEO_INIT LDA #VR_H_DISPLAYED
	STA VIDEO_ADDR
	LDA #V_COLS
	STA VIDEO_DATA
	LDA #VR_V_DISPLAYED
	STA VIDEO_ADDR
	LDA #V_ROWS
	STA VIDEO_DATA
	LDA #VR_CURSOR_START
	STA VIDEO_ADDR
	LDA #CURSOR_ON
	STA VIDEO_DATA
	LDA #(VIDEO_RAM & $FF)
	STA V_ADDR_LO
	STA ADDRLO
	LDA #(VIDEO_RAM >> 8)
	STA V_ADDR_HI
	STA ADDRHI
	LDA #0
	STA V_COL
	LDX #0
VI_ROW_LOOP LDY #0
VI_BYTE_LOOP LDA #' '
	STA (ADDRLO),Y
	INY
	CPY #V_COLS
	BNE VI_BYTE_LOOP
	LDA ADDRLO
	CLC
	ADC #V_COLS
	STA ADDRLO
	LDA ADDRHI
	ADC #0
	STA ADDRHI
	INX
	CPX #V_ROWS
	BNE VI_ROW_LOOP
	JSR PC_SET_CURSOR
	RTS

DO_GETCH LDA VIDEO_KBD_STAT
	AND #DATA_AVAILABLE
	BEQ DO_GETCH
	LDA VIDEO_KBD_DATA
	RTS

DO_SELECT STA FD_DRIVE
	RTS

;; ---- A = track, X = sector, ADDRLO/ADDRHI = destination address. The Read
;;      Sector command loads its whole 128-byte buffer synchronously, so this
;;      can just drain FD_DATA in a tight loop - no DRQ/interrupt handling
;;      needed for a simple polling reader. ----
DO_READ_SECTOR STA FD_TRACK
	STX FD_SECTOR
	LDA #$80
	STA FD_STATUS
	LDY #0
DRS_LOOP LDA FD_DATA
	STA (ADDRLO),Y
	INY
	CPY #128
	BNE DRS_LOOP
	RTS

;; ---- A = track, X = sector, ADDRLO/ADDRHI = source address. Mirrors
;;      DO_READ_SECTOR in reverse - feeding FD_DATA drives the controller's
;;      Write Sector transfer, which flushes to the backing file on the 128th
;;      byte. ----
DO_WRITE_SECTOR STA FD_TRACK
	STX FD_SECTOR
	LDA #$A0
	STA FD_STATUS
	LDY #0
DWS_LOOP LDA (ADDRLO),Y
	STA FD_DATA
	INY
	CPY #128
	BNE DWS_LOOP
	RTS

;; ---- A = track, X = fill byte. One track only, like a real WD179x Write
;;      Track - a caller wanting the whole disk formatted loops over every
;;      track itself (see MONITOR.asm's F command). Runs to completion
;;      synchronously, like Seek/Restore, so there's no real BUSY period to
;;      poll. ----
DO_FORMAT_TRACK STA FD_TRACK
	STX FD_DATA
	LDA #$F0
	STA FD_STATUS
	RTS

;; ---- home the selected drive's head to track 0. No parameters - restore
;;      doesn't need a target track like seek does. Used by the monitor's
;;      reset path to put hardware into a known state regardless of whatever
;;      a previous program left the drive/track selection as. ----
DO_RESTORE LDA #$00
	STA FD_STATUS
	RTS
