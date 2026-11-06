;; Monitor ROM: COMMAND ARGUMENTS style, read from and echoed to the console.
;; Never touches hardware registers directly - every console/disk access goes
;; through BIOS.asm's fixed jump table (BIOS_* below). This ROM owns the reset
;; path and the fault-recovery policy ("print ? and return to the prompt"),
;; since both are monitor behavior, not hardware primitives.
;;   D <addr>                    dump 256 bytes starting at addr, hexdump -C style
;;   W <addr> <byte> [byte...]   write one or more bytes starting at addr
;;   H                           list commands
;;   E <addr>                    execute code (JSR); the routine should end in RTS
;;   S <drive>                   select floppy drive 0-3
;;   F [fill]                    format every track on the selected drive (default fill $00)
;;   L <track> <sector> <addr>   load one 128-byte sector into memory
;;   P <track> <sector> <addr>   write (put) one 128-byte sector from memory to disk
;;   B                           boot: find CCP.COM in the filesystem (via BDOS -
;;                               see bdos.c) and load it to $0300, then JSR there.
;;                               Track 0 is no longer read at all - CCP.COM is a
;;                               regular file on tracks 1+, found by name, the
;;                               same way real CP/M's cold loader worked.
CPU 6502
OUTPUT HEX

;; BIOS primitives ROM jump table (see BIOS.asm) - frozen ABI, keep in sync.
BIOS_PUTCH        = $F000	;; A = char
BIOS_GETCH        = $F003	;; blocks; returns A = char
BIOS_SELECT       = $F006	;; A = drive 0-3
BIOS_READ_SECTOR  = $F009	;; A = track, X = sector, ADDRLO/ADDRHI = dest
BIOS_WRITE_SECTOR = $F00C	;; A = track, X = sector, ADDRLO/ADDRHI = src
BIOS_FORMAT_TRACK = $F00F	;; A = track, X = fill byte
BIOS_RESTORE      = $F012	;; no params - homes the selected drive to track 0

;; BDOS ROM jump table (see bdos_jumptable.s/bdos.c) - a second, separately
;; assembled/compiled frozen ABI, same keep-in-sync contract as BIOS above.
BDOS_FIND_FILE    = $A006	;; ADDRLO/ADDRHI = null-terminated filename to find;
				;; returns A = 1 found / 0 not found
BDOS_READ_RECORD  = $A009	;; ADDRLO/ADDRHI = 128-byte dest; returns A = 1 read / 0 done

TRACKS = 76			;; IBM3740 geometry - the only drive type this BIOS knows about
BOOTADDR = $0300		;; where B loads CCP.COM's content - past INBUF ($0200-$0226),
				;; well clear of this program's own code at $F800+

INPTR    = $00		;; zero page: current parse offset into INBUF
ADDRLO   = $01		;; zero page: parsed address, low byte - doubles as an indirect
			;; pointer, and as the BIOS_READ/WRITE_SECTOR address parameter
ADDRHI   = $02		;; zero page: parsed address, high byte (ADDRLO+1, required for (ADDRLO),Y)
BYTEVAL  = $03		;; zero page: parsed byte value for W/S/F's fill byte/L/P's sector
TRACKVAL = $04		;; zero page: parsed track value for L/P, loop counter for F

;; zero page: E's self-modifying-JSR scratch area (see CMD_EXEC). This has to be
;; writable RAM, not ROM - it used to live as DB bytes right in this program's own
;; code, back when the whole thing loaded via plain bus writes into RAM. Now that
;; this ROM is genuinely write-protected, CMD_EXEC writes all four bytes fresh on
;; every call instead of relying on them being pre-baked at assembly time.
EXEC_JSR = $05		;; JSR opcode
EXEC_LO  = $06		;; low byte of the call target
EXEC_HI  = $07		;; high byte of the call target
EXEC_RTS = $08		;; RTS opcode - where the called routine's own RTS lands

INBUF = $0200
;; 39 was too short in practice - a W/P command listing even a dozen-ish hex bytes
;; (e.g. a short boot routine, poked byte by byte) runs past it with no visible
;; warning: READLINE silently stops echoing/accepting at MAXLEN, but the rest of
;; what was typed is still queued and gets consumed as the START of the NEXT line,
;; which usually doesn't begin with a valid command letter - hence an unexpected
;; "?" after pressing enter. 120 comfortably covers that case and stays well
;; within the reserved INBUF page ($0200-$02FF), without reaching into BOOTADDR's
;; page at $0300.
MAXLEN = 120

* = $F800

;; ---- reset/NMI entry point (see the vector table at the bottom of this file):
;;      puts hardware into a known state - drive 0, head restored to track 0 -
;;      before falling into the normal prompt loop. Whatever a previous program
;;      (a future DOS, say) left the drive/track selection as shouldn't carry
;;      over across a reset, even though the monitor's own ROM code never
;;      needed "restoring" itself (see Doc/Architecture.md - ROM is write-
;;      protected and the reset vector is unconditional, so the monitor was
;;      never actually torn down, just not the thing running). Plain LOOP
;;      (reached by JMP from FAULT and from LOOP's own repeat) intentionally
;;      skips this - those shouldn't re-home the drive on every command. ----
RESET_INIT LDA #0
	JSR BIOS_SELECT
	JSR BIOS_RESTORE
	JMP LOOP

;; LOOP is also a reset/NMI target (indirectly, via RESET_INIT above) - embedded
;; directly in this ROM's own vector table below, loaded via basicROM's
;; privileged constructor path - no separate Java-side setVectors() call
;; needed, same as RTCInt.hex.
LOOP	JSR PROMPT
	JSR READLINE
	JSR DISPATCH
	JMP LOOP

PROMPT	LDA #'>'
	JSR BIOS_PUTCH
	LDA #' '
	JSR BIOS_PUTCH
	RTS

NEWLINE	LDA #$0A
	JSR BIOS_PUTCH
	RTS

;; ---- read a line into INBUF, echoing, until CR or LF; null terminate; reset INPTR.
;;      Swing's keyTyped for Enter isn't consistently '\r' across platforms/toolkits,
;;      so both line endings are accepted here rather than just $0D. Backspace ($08)
;;      or DEL ($7F) erases the last character, if there is one - echoing BS,space,BS
;;      to visually erase it, same as any real dumb terminal. ----
READLINE LDY #0
RL_LOOP	JSR BIOS_GETCH
	CMP #$0D
	BEQ RL_DONE
	CMP #$0A
	BEQ RL_DONE
	CMP #$08
	BEQ RL_BS
	CMP #$7F
	BEQ RL_BS
	STA INBUF,Y
	JSR BIOS_PUTCH
	INY
	CPY #MAXLEN
	BNE RL_LOOP
RL_DONE	LDA #0
	STA INBUF,Y
	JSR NEWLINE
	LDA #0
	STA INPTR
	RTS
RL_BS	CPY #0
	BEQ RL_LOOP	;; nothing to erase at the start of the line
	DEY
	LDA #$08
	JSR BIOS_PUTCH
	LDA #' '
	JSR BIOS_PUTCH
	LDA #$08
	JSR BIOS_PUTCH
	JMP RL_LOOP

;; ---- skip spaces in INBUF starting at INPTR ----
SKIPSP	LDY INPTR
SKIPSP1	LDA INBUF,Y
	CMP #' '
	BNE SKIPSPE
	INY
	BNE SKIPSP1
SKIPSPE	STY INPTR
	RTS

;; ---- parse one hex digit at INBUF,INPTR into A (0-15), advancing INPTR.
;;      returns A=$FF and leaves INPTR unchanged if the character isn't hex.
HEXDIGIT LDY INPTR
	LDA INBUF,Y
	CMP #'0'
	BCC HD_FAIL
	CMP #':'
	BCS HD_ALPHA
	SEC
	SBC #'0'
	INC INPTR
	RTS
HD_ALPHA CMP #'A'
	BCC HD_FAIL
	CMP #'G'
	BCS HD_LOWER
	SEC
	SBC #'7'	;; 'A'-10, precomputed
	INC INPTR
	RTS
HD_LOWER CMP #'a'
	BCC HD_FAIL
	CMP #'g'
	BCS HD_FAIL
	SEC
	SBC #'W'	;; 'a'-10, precomputed
	INC INPTR
	RTS
HD_FAIL	LDA #$FF
	RTS

;; ---- skip a single optional leading '$' (the usual 6502 hex-literal marker) at INPTR ----
SKIPDOLLAR LDY INPTR
	LDA INBUF,Y
	CMP #'$'
	BNE SD_DONE
	INC INPTR
SD_DONE	RTS

;; ---- parse hex digits starting at INPTR into ADDRLO/ADDRHI (16-bit) ----
PARSEADDR LDA #0
	STA ADDRLO
	STA ADDRHI
	JSR SKIPDOLLAR
PA_LOOP	JSR HEXDIGIT
	CMP #$FF
	BEQ PA_DONE
	PHA
	ASL ADDRLO
	ROL ADDRHI
	ASL ADDRLO
	ROL ADDRHI
	ASL ADDRLO
	ROL ADDRHI
	ASL ADDRLO
	ROL ADDRHI
	PLA
	ORA ADDRLO
	STA ADDRLO
	JMP PA_LOOP
PA_DONE	RTS

;; ---- parse hex digits starting at INPTR into BYTEVAL (8-bit) ----
PARSEBYTE LDA #0
	STA BYTEVAL
	JSR SKIPDOLLAR
PB_LOOP	JSR HEXDIGIT
	CMP #$FF
	BEQ PB_DONE
	PHA
	ASL BYTEVAL
	ASL BYTEVAL
	ASL BYTEVAL
	ASL BYTEVAL
	PLA
	ORA BYTEVAL
	STA BYTEVAL
	JMP PB_LOOP
PB_DONE	RTS

;; ---- print A as 2 hex digits ----
PRHEX8	PHA
	LSR A
	LSR A
	LSR A
	LSR A
	JSR PRNIB
	PLA
	AND #$0F
	JSR PRNIB
	RTS

PRNIB	AND #$0F
	CMP #10
	BCC PN_DIGIT
	CLC
	ADC #'7'	;; 'A'-10, precomputed - nibble 10 -> 'A'
	JMP PN_OUT
PN_DIGIT CLC
	ADC #'0'
PN_OUT	JSR BIOS_PUTCH
	RTS

;; ---- print ADDRHI:ADDRLO as 4 hex digits ----
PRADDR	LDA ADDRHI
	JSR PRHEX8
	LDA ADDRLO
	JSR PRHEX8
	RTS

;; ---- dispatch on the first non-space character of INBUF. Every comparison branches
;;      to a small, nearby "DJ_" stub that itself does an unconditional JMP to the real
;;      handler, rather than branching to the handler directly - a direct BEQ has to
;;      reach its target in a single signed byte (-128..127), and with this many
;;      commands the handler blocks for letters checked early in the chain (D, W, H...)
;;      land well outside that range. JMP has no such limit, so indirecting through a
;;      one-instruction stub sidesteps the problem for good instead of needing another
;;      one-off invert-and-JMP fix every time a command is added. ----
DISPATCH LDA #0
	STA INPTR
	JSR SKIPSP
	LDY INPTR
	LDA INBUF,Y
	BNE DI_HASCMD	;; empty line
	JMP DI_DONE
DI_HASCMD INY
	STY INPTR
	CMP #'D'
	BEQ DJ_DUMP
	CMP #'W'
	BEQ DJ_WRITE
	CMP #'H'
	BEQ DJ_HELP
	CMP #'E'
	BEQ DJ_EXEC
	CMP #'S'
	BEQ DJ_SELECT
	CMP #'F'
	BEQ DJ_FORMAT
	CMP #'L'
	BEQ DJ_LOAD
	CMP #'P'
	BEQ DJ_PUT
	CMP #'B'
	BEQ DJ_BOOT
	JSR HUH
	RTS
DJ_DUMP	JMP DI_DUMP
DJ_WRITE JMP DI_WRITE
DJ_HELP	JMP DI_HELP
DJ_EXEC	JMP DI_EXEC
DJ_SELECT JMP DI_SELECT
DJ_FORMAT JMP DI_FORMAT
DJ_LOAD	JMP DI_LOAD
DJ_PUT	JMP DI_PUT
DJ_BOOT	JMP DI_BOOT

DI_DUMP	JSR SKIPSP
	JSR PARSEADDR
	JSR CMD_DUMP
	RTS
DI_WRITE JSR SKIPSP
	JSR PARSEADDR
	JSR CW_LOOP
	RTS
DI_HELP	JSR CMD_HELP
	RTS
DI_EXEC	JSR SKIPSP
	JSR PARSEADDR
	JSR CMD_EXEC
	RTS
DI_SELECT JSR SKIPSP
	JSR PARSEBYTE
	LDA BYTEVAL
	JSR BIOS_SELECT
	RTS
;; ---- F: format every track on the selected drive. An optional fill byte follows
;;      the command letter; if INBUF is exhausted here, default to $00. ----
DI_FORMAT JSR SKIPSP
	LDA #0
	STA BYTEVAL
	LDY INPTR
	LDA INBUF,Y
	BEQ DF_NOARG
	JSR PARSEBYTE
DF_NOARG JSR CMD_FORMAT
	RTS
;; ---- L: parse track/sector/addr, then read the sector straight through the
;;      BIOS primitive - A=track, X=sector, ADDRLO/ADDRHI=dest (set by
;;      PARSEADDR). TRACKVAL holds track across the second PARSEBYTE call,
;;      since that call reuses BYTEVAL for the sector. ----
DI_LOAD	JSR SKIPSP
	JSR PARSEBYTE
	LDA BYTEVAL
	STA TRACKVAL
	JSR SKIPSP
	JSR PARSEBYTE
	JSR SKIPSP
	JSR PARSEADDR
	LDA TRACKVAL
	LDX BYTEVAL
	JSR BIOS_READ_SECTOR
	RTS
;; ---- P: same parse as L, but write memory to disk instead. ----
DI_PUT	JSR SKIPSP
	JSR PARSEBYTE
	LDA BYTEVAL
	STA TRACKVAL
	JSR SKIPSP
	JSR PARSEBYTE
	JSR SKIPSP
	JSR PARSEADDR
	LDA TRACKVAL
	LDX BYTEVAL
	JSR BIOS_WRITE_SECTOR
	RTS
;; ---- B: boot - find CCP.COM by name via BDOS (see bdos.c/bdos_jumptable.s)
;;      and load its content to BOOTADDR, then JSR there - real CP/M's cold
;;      loader worked the same way (find the CCP in the directory, not a
;;      fixed, hand-maintained boot area). Track 0 is no longer read by B at
;;      all; CCP.COM is an ordinary file on tracks 1+, written there with
;;      CpmDiskTool's "put" subcommand ahead of time. Unlike E, BOOTADDR is a
;;      fixed, assemble-time constant, so this needs none of E's self-
;;      modifying-JSR trick - just a direct "JSR BOOTADDR" once loading is
;;      done. A loaded program should end in RTS to return to the monitor,
;;      same convention as E; a real shell meant to take over the machine
;;      entirely can simply never return. ----
DI_BOOT	LDA #0
	JSR BIOS_SELECT
	JSR BIOS_RESTORE

	;; cc65's software stack pointer (zero page $0A/$0B, per bdos.map's
	;; exports list) - BDOS's own crt0 never runs (nothing JSRs to its
	;; STARTUP segment, only to its jump table), so nothing else ever
	;; initializes this before BDOS's compiled code needs it for parameter
	;; passing and the track/sector division - BDOS_FIND_FILE's first-ever
	;; call would otherwise run with a garbage stack pointer, scribbling
	;; wherever that garbage address happens to point (caught via a
	;; "RO Memory Write attempt" flood at runtime). $8000 matches
	;; simplecpu.cfg's own __STACKSTART__, so CCP.COM's crt0 will set the
	;; exact same value once it's running - set only here, at the first-
	;; ever BDOS call, not on every DI_BOOT entry or call into BDOS, since
	;; resetting it after CCP.COM has pushed its own data would corrupt
	;; CCP's in-progress stack frame.
	LDA #$00
	STA $0A
	LDA #$80
	STA $0B

	LDA CCPNAME_PTR
	STA ADDRLO
	LDA CCPNAME_PTR+1
	STA ADDRHI
	JSR BDOS_FIND_FILE
	CMP #0
	BEQ DB_NOTFOUND

	;; BYTEVAL/TRACKVAL track the running destination address here - not
	;; ADDRLO/ADDRHI directly. BDOS_READ_RECORD's own implementation calls
	;; BIOS_READ_SECTOR internally (to pull the next disk sector into its
	;; own buffer), which stages ITS OWN pointer into ADDRLO/ADDRHI before
	;; returning - so by the time control comes back here, ADDRLO/ADDRHI
	;; points at BDOS's sector buffer, not wherever we last told it to
	;; write. Restaged into ADDRLO/ADDRHI fresh before every call instead
	;; (same reason ccp.c's cmd_dir/cmd_type restage bios_set_buffer()
	;; before every bdos_next_entry()/bdos_read_record() call). BYTEVAL/
	;; TRACKVAL are free during B - neither is used until a byte/track
	;; value actually needs parsing, which B's own command syntax has none
	;; of.
	LDA #$00
	STA BYTEVAL
	LDA #$03
	STA TRACKVAL
DB_LOOP	LDA BYTEVAL
	STA ADDRLO
	LDA TRACKVAL
	STA ADDRHI
	JSR BDOS_READ_RECORD
	CMP #0
	BEQ DB_RUN		;; no more records - loading is done
	LDA BYTEVAL
	CLC
	ADC #$80		;; next 128-byte chunk
	STA BYTEVAL
	BCC DB_NOCARRY
	INC TRACKVAL
DB_NOCARRY JMP DB_LOOP

DB_NOTFOUND LDA #'?'
	JSR BIOS_PUTCH
	JSR NEWLINE
	RTS

DB_RUN	JSR BOOTADDR
DI_DONE	RTS

;; CCPNAME_PTR holds CCP.COM's own address, computed by crasm itself at
;; assembly time (same DW mechanism the vector table at the bottom of this
;; file already relies on) - avoids needing any "load the low/high byte of
;; a label" immediate-operand syntax, which the rest of this file never
;; uses either.
CCPNAME_PTR DW CCPNAME
CCPNAME	ASC "CCP.COM\0"

HUH	LDA #'?'
	JSR BIOS_PUTCH
	JSR NEWLINE
	RTS

;; ---- H: print a short command summary. Small unrolled print loops rather than one
;;      shared subroutine - a shared one would need either an indirect zero-page
;;      pointer (clobbering ADDRLO/ADDRHI) or low/high-byte address-splitting operators,
;;      and this avoids relying on either being supported. ----
CMD_HELP LDY #0
CH_D	LDA HELPD,Y
	BEQ CH_D_DONE
	JSR BIOS_PUTCH
	INY
	BNE CH_D
CH_D_DONE JSR NEWLINE
	LDY #0
CH_W	LDA HELPW,Y
	BEQ CH_W_DONE
	JSR BIOS_PUTCH
	INY
	BNE CH_W
CH_W_DONE JSR NEWLINE
	LDY #0
CH_H	LDA HELPH,Y
	BEQ CH_H_DONE
	JSR BIOS_PUTCH
	INY
	BNE CH_H
CH_H_DONE JSR NEWLINE
	LDY #0
CH_E	LDA HELPE,Y
	BEQ CH_E_DONE
	JSR BIOS_PUTCH
	INY
	BNE CH_E
CH_E_DONE JSR NEWLINE
	LDY #0
CH_S	LDA HELPS,Y
	BEQ CH_S_DONE
	JSR BIOS_PUTCH
	INY
	BNE CH_S
CH_S_DONE JSR NEWLINE
	LDY #0
CH_F	LDA HELPF,Y
	BEQ CH_F_DONE
	JSR BIOS_PUTCH
	INY
	BNE CH_F
CH_F_DONE JSR NEWLINE
	LDY #0
CH_L	LDA HELPL,Y
	BEQ CH_L_DONE
	JSR BIOS_PUTCH
	INY
	BNE CH_L
CH_L_DONE JSR NEWLINE
	LDY #0
CH_P	LDA HELPP,Y
	BEQ CH_P_DONE
	JSR BIOS_PUTCH
	INY
	BNE CH_P
CH_P_DONE JSR NEWLINE
	LDY #0
CH_B	LDA HELPB,Y
	BEQ CH_B_DONE
	JSR BIOS_PUTCH
	INY
	BNE CH_B
CH_B_DONE JSR NEWLINE
	RTS

HELPD	ASC "D addr          dump 256 bytes\0"
HELPW	ASC "W addr byte...  write one or more bytes\0"
HELPH	ASC "H               this help\0"
HELPE	ASC "E addr          execute (JSR); return via RTS\0"
HELPS	ASC "S drive         select floppy drive 0-3\0"
HELPF	ASC "F [fill]        format every track (default $00)\0"
HELPL	ASC "L track sec addr  load one sector into memory\0"
HELPP	ASC "P track sec addr  write one sector from memory\0"
HELPB	ASC "B               boot: find and run CCP.COM\0"

;; ---- D: dump 256 bytes starting at ADDRHI:ADDRLO, hexdump -C style ----
CMD_DUMP LDX #16
CD_LINE	JSR PRADDR
	LDA #' '
	JSR BIOS_PUTCH
	JSR BIOS_PUTCH
	LDY #0
CD_HEX	LDA (ADDRLO),Y
	JSR PRHEX8
	LDA #' '
	JSR BIOS_PUTCH
	INY
	TYA		;; every 8th byte (incl. the 16th) gets a second gap space,
	AND #$07	;; matching real hexdump -C - not just the midpoint
	BNE CD_H8
	LDA #' '
	JSR BIOS_PUTCH
CD_H8	CPY #16
	BNE CD_HEX
	LDA #'|'
	JSR BIOS_PUTCH
	LDY #0
CD_ASC	LDA (ADDRLO),Y
	CMP #$20
	BCC CD_DOT
	CMP #$7F
	BCC CD_PRCH
CD_DOT	LDA #'.'
CD_PRCH	JSR BIOS_PUTCH
	INY
	CPY #16
	BNE CD_ASC
	LDA #'|'
	JSR BIOS_PUTCH
	JSR NEWLINE
	LDA ADDRLO
	CLC
	ADC #16
	STA ADDRLO
	BCC CD_NC
	INC ADDRHI
CD_NC	DEX
	BEQ CD_EXIT	;; 16 lines is too long for a direct backward branch - invert + JMP instead
	JMP CD_LINE
CD_EXIT	RTS

;; ---- W: parse one or more hex byte values after the address, write them consecutively.
;;      Entered via "JSR CW_LOOP" from DISPATCH, after PARSEADDR has set up ADDRLO/ADDRHI. ----
CW_LOOP	JSR SKIPSP
	LDY INPTR
	LDA INBUF,Y
	BEQ CW_DONE
	JSR PARSEBYTE
	LDA BYTEVAL
	LDY #0
	STA (ADDRLO),Y
	INC ADDRLO
	BNE CW_LOOP
	INC ADDRHI
	JMP CW_LOOP
CW_DONE	RTS

;; ---- E: execute code at ADDRHI:ADDRLO. The 6502 has no "JSR to a runtime-computed
;;      address" opcode - JSR only takes a literal operand - so this writes a fresh
;;      JSR instruction into the zero-page scratch area above before calling it, the
;;      classic monitor-ROM self-modifying-code technique - except the scratch bytes
;;      have to live in zero page now rather than inline in this program's own code,
;;      since this ROM is genuinely write-protected. Because it's a real JSR, a called
;;      routine ending in RTS returns cleanly all the way back to the prompt; HLT or an
;;      infinite loop would not (HLT halts the whole CPU - see CPU.run()'s
;;      illegalOpCodeException handling - and is meant for unit tests signalling "this
;;      program is done", not for returning control to a running monitor). ----
CMD_EXEC LDA #$20
	STA EXEC_JSR
	LDA ADDRLO
	STA EXEC_LO
	LDA ADDRHI
	STA EXEC_HI
	LDA #$60
	STA EXEC_RTS
	JSR EXEC_JSR
	RTS

;; ---- F: format every track on the selected drive, one BIOS_FORMAT_TRACK call per
;;      track. BYTEVAL already holds the fill byte (set by DI_FORMAT, defaulting to
;;      $00 if none was typed); TRACKVAL is the loop counter. ----
CMD_FORMAT LDA #0
	STA TRACKVAL
CF_LOOP	LDA TRACKVAL
	LDX BYTEVAL
	JSR BIOS_FORMAT_TRACK
	LDA TRACKVAL
	CLC
	ADC #1
	STA TRACKVAL
	CMP #TRACKS
	BNE CF_LOOP
	RTS

;; ---- bus fault handler (CPU.VECTOR_FAULT, $FFF8): entered via a pushed PC+PSR like a
;;      real interrupt, but there's no sense resuming the faulting instruction (it would
;;      just fault again), so this discards the pushed state and returns straight to the
;;      prompt loop instead of attempting RTI. ----
FAULT	PLA		;; discard the pushed PSR
	PLA		;; discard the pushed PCL
	PLA		;; discard the pushed PCH
	JSR HUH		;; reuse the same "?" + newline the unknown-command path prints
	JMP LOOP

;; Hardware vectors plus our project-specific fault vector, embedded directly:
;; this ROM loads via basicROM's privileged constructor-time path (bypassing
;; write protection), same as RTCInt.hex, so it carries its own vector table -
;; no separate Java-side setVectors()/setFaultVector() call needed, and
;; neither FAULT nor RESET_INIT need to live at any particular fixed address -
;; DW resolves each one's real address automatically. NMI and RESET both go
;; through RESET_INIT (a "start over" event should re-home the hardware);
;; IRQ isn't meaningfully used by this system yet, and conceptually shouldn't
;; re-init hardware mid-operation even if it were, so it goes straight to LOOP.
* = $FFF8
	DW FAULT	;; $FFF8 fault vector (CPU.VECTOR_FAULT, project-specific)
	DW RESET_INIT	;; $FFFA NMI
	DW RESET_INIT	;; $FFFC RESET
	DW LOOP		;; $FFFE IRQ
