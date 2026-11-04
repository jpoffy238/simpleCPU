;; Primitives ROM: raw hardware access only - console I/O and disk I/O - no
;; command parsing or filesystem concepts. That's MONITOR.asm's job; it calls
;; these services exclusively through the fixed jump table below, never
;; touching CONSOLE_DATA/FD_STATUS/etc. itself. This file and MONITOR.asm are
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
CPU 6502
OUTPUT HEX

CONSOLE_DATA = $EC00
CONSOLE_STAT = $EC01
DATA_AVAILABLE = $01

;; FloppyController registers (see FloppyController.java)
FD_STATUS = $9010		;; status (read) / command (write)
FD_TRACK  = $9011
FD_SECTOR = $9012
FD_DATA   = $9013
FD_DRIVE  = $9014

;; zero page: shared with MONITOR.asm - the sector-buffer address parameter for
;; BIOS_READ_SECTOR/BIOS_WRITE_SECTOR. Part of the frozen calling convention.
ADDRLO = $01
ADDRHI = $02

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

DO_PUTCH STA CONSOLE_DATA
	RTS

DO_GETCH LDA CONSOLE_STAT
	AND #DATA_AVAILABLE
	BEQ DO_GETCH
	LDA CONSOLE_DATA
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
