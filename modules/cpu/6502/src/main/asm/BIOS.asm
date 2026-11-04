;; Simple monitor BIOS: COMMAND ARGUMENTS style, read from and echoed to the console.
;;   D <addr>                    dump 256 bytes starting at addr, hexdump -C style
;;   W <addr> <byte> [byte...]   write one or more bytes starting at addr
;;   H                           list commands
;;   E <addr>                    execute code (JSR); the routine should end in RTS
;;   S <drive>                   select floppy drive 0-3
;;   F [fill]                    format every track on the selected drive (default fill $00)
;;   L <track> <sector> <addr>   load one 128-byte sector into memory
;;   P <track> <sector> <addr>   write (put) one 128-byte sector from memory to disk
;;   B                           boot: load track 0 sector 1 from the selected drive
;;                               to $0300 and JSR there
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
TRACKS = 76			;; IBM3740 geometry - the only drive type this BIOS knows about
BOOTADDR = $0300		;; where B loads the boot sector - past INBUF ($0200-$0226), well
				;; clear of this program's own code at $1000+

INPTR   = $00		;; zero page: current parse offset into INBUF
ADDRLO  = $01		;; zero page: parsed address, low byte - doubles as an indirect pointer
ADDRHI  = $02		;; zero page: parsed address, high byte (ADDRLO+1, required for (ADDRLO),Y)
BYTEVAL = $03		;; zero page: parsed byte value for W

INBUF = $0200
MAXLEN = 39

* = $1000

;; LOOP is the reset target (set directly via rom.setVectors($1000,...) in Java,
;; not by name) since * = $1000 already pins this as the program's first instruction.
LOOP	JSR PROMPT
	JSR READLINE
	JSR DISPATCH
	JMP LOOP

PROMPT	LDA #'>'
	JSR PUTCH
	LDA #' '
	JSR PUTCH
	RTS

;; ---- console I/O ----
GETCH	LDA CONSOLE_STAT
	AND #DATA_AVAILABLE
	BEQ GETCH
	LDA CONSOLE_DATA
	RTS

PUTCH	STA CONSOLE_DATA
	RTS

NEWLINE	LDA #$0A
	JSR PUTCH
	RTS

;; ---- read a line into INBUF, echoing, until CR or LF; null terminate; reset INPTR.
;;      Swing's keyTyped for Enter isn't consistently '\r' across platforms/toolkits,
;;      so both line endings are accepted here rather than just $0D. Backspace ($08)
;;      or DEL ($7F) erases the last character, if there is one - echoing BS,space,BS
;;      to visually erase it, same as any real dumb terminal. ----
READLINE LDY #0
RL_LOOP	JSR GETCH
	CMP #$0D
	BEQ RL_DONE
	CMP #$0A
	BEQ RL_DONE
	CMP #$08
	BEQ RL_BS
	CMP #$7F
	BEQ RL_BS
	STA INBUF,Y
	JSR PUTCH
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
	JSR PUTCH
	LDA #' '
	JSR PUTCH
	LDA #$08
	JSR PUTCH
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
PN_OUT	JSR PUTCH
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
;;      one-off invert-and-JMP fix every time a command is added (see CD_LINE/CD_EXIT
;;      and DI_DONE above for the one-off version of this same fix). ----
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
	STA FD_DRIVE
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
DI_LOAD	JSR SKIPSP
	JSR PARSEBYTE
	LDA BYTEVAL
	STA FD_TRACK
	JSR SKIPSP
	JSR PARSEBYTE
	LDA BYTEVAL
	STA FD_SECTOR
	JSR SKIPSP
	JSR PARSEADDR
	JSR CMD_LOAD
	RTS
DI_PUT	JSR SKIPSP
	JSR PARSEBYTE
	LDA BYTEVAL
	STA FD_TRACK
	JSR SKIPSP
	JSR PARSEBYTE
	LDA BYTEVAL
	STA FD_SECTOR
	JSR SKIPSP
	JSR PARSEADDR
	JSR CMD_SAVE
	RTS
;; ---- B: boot - load track 0, sector 1 from the selected drive to BOOTADDR and JSR
;;      there. Unlike E, BOOTADDR is a fixed, assemble-time constant, so this needs
;;      none of E's self-modifying-JSR trick - just a direct "JSR BOOTADDR". A loaded
;;      routine should end in RTS to return to the monitor, same convention as E; a
;;      real boot sector meant to take over the machine entirely can simply never
;;      return. ----
DI_BOOT	LDA #0
	STA FD_TRACK
	LDA #1
	STA FD_SECTOR
	LDA #$00
	STA ADDRLO
	LDA #$03
	STA ADDRHI
	JSR CMD_LOAD
	JSR BOOTADDR
DI_DONE	RTS

HUH	LDA #'?'
	JSR PUTCH
	JSR NEWLINE
	RTS

;; ---- H: print a short command summary. Small unrolled print loops rather than one
;;      shared subroutine - a shared one would need either an indirect zero-page
;;      pointer (clobbering ADDRLO/ADDRHI) or low/high-byte address-splitting operators,
;;      and this avoids relying on either being supported. ----
CMD_HELP LDY #0
CH_D	LDA HELPD,Y
	BEQ CH_D_DONE
	JSR PUTCH
	INY
	BNE CH_D
CH_D_DONE JSR NEWLINE
	LDY #0
CH_W	LDA HELPW,Y
	BEQ CH_W_DONE
	JSR PUTCH
	INY
	BNE CH_W
CH_W_DONE JSR NEWLINE
	LDY #0
CH_H	LDA HELPH,Y
	BEQ CH_H_DONE
	JSR PUTCH
	INY
	BNE CH_H
CH_H_DONE JSR NEWLINE
	LDY #0
CH_E	LDA HELPE,Y
	BEQ CH_E_DONE
	JSR PUTCH
	INY
	BNE CH_E
CH_E_DONE JSR NEWLINE
	LDY #0
CH_S	LDA HELPS,Y
	BEQ CH_S_DONE
	JSR PUTCH
	INY
	BNE CH_S
CH_S_DONE JSR NEWLINE
	LDY #0
CH_F	LDA HELPF,Y
	BEQ CH_F_DONE
	JSR PUTCH
	INY
	BNE CH_F
CH_F_DONE JSR NEWLINE
	LDY #0
CH_L	LDA HELPL,Y
	BEQ CH_L_DONE
	JSR PUTCH
	INY
	BNE CH_L
CH_L_DONE JSR NEWLINE
	LDY #0
CH_P	LDA HELPP,Y
	BEQ CH_P_DONE
	JSR PUTCH
	INY
	BNE CH_P
CH_P_DONE JSR NEWLINE
	LDY #0
CH_B	LDA HELPB,Y
	BEQ CH_B_DONE
	JSR PUTCH
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
HELPB	ASC "B               boot track 0 sector 1 from the drive\0"

;; ---- D: dump 256 bytes starting at ADDRHI:ADDRLO, hexdump -C style ----
CMD_DUMP LDX #16
CD_LINE	JSR PRADDR
	LDA #' '
	JSR PUTCH
	JSR PUTCH
	LDY #0
CD_HEX	LDA (ADDRLO),Y
	JSR PRHEX8
	LDA #' '
	JSR PUTCH
	INY
	TYA		;; every 8th byte (incl. the 16th) gets a second gap space,
	AND #$07	;; matching real hexdump -C - not just the midpoint
	BNE CD_H8
	LDA #' '
	JSR PUTCH
CD_H8	CPY #16
	BNE CD_HEX
	LDA #'|'
	JSR PUTCH
	LDY #0
CD_ASC	LDA (ADDRLO),Y
	CMP #$20
	BCC CD_DOT
	CMP #$7F
	BCC CD_PRCH
CD_DOT	LDA #'.'
CD_PRCH	JSR PUTCH
	INY
	CPY #16
	BNE CD_ASC
	LDA #'|'
	JSR PUTCH
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
;;      address" opcode - JSR only takes a literal operand - so this patches the target
;;      bytes of a fixed JSR placeholder before calling it, the classic monitor-ROM
;;      self-modifying-code technique. Because it's a real JSR, a called routine ending
;;      in RTS returns cleanly all the way back to the prompt; HLT or an infinite loop
;;      would not (HLT halts the whole CPU - see CPU.run()'s illegalOpCodeException
;;      handling - and is meant for unit tests signalling "this program is done", not
;;      for returning control to a running monitor). ----
CMD_EXEC LDA ADDRLO
	STA EXEC_LO
	LDA ADDRHI
	STA EXEC_HI
	JSR EXEC_JSR
	RTS
EXEC_JSR DB $20		;; JSR opcode
EXEC_LO	DB $00		;; patched above - low byte of the call target
EXEC_HI	DB $00		;; patched above - high byte of the call target
	RTS		;; where the called routine's own RTS lands

;; ---- F: stage the fill byte (parsed by DI_FORMAT into BYTEVAL, defaulting to $00)
;;      into the Data register, then issue a Format Track command for every track
;;      on the selected drive. Each Format Track command runs to completion
;;      synchronously - like Seek/Restore, there's no real BUSY period to poll. ----
CMD_FORMAT LDA BYTEVAL
	STA FD_DATA
	LDA #0
	STA FD_TRACK
CF_LOOP	LDA #$F0
	STA FD_STATUS
	LDA FD_TRACK
	CLC
	ADC #1
	STA FD_TRACK
	CMP #TRACKS
	BNE CF_LOOP
	RTS

;; ---- L: read one 128-byte sector from the selected drive (FD_TRACK/FD_SECTOR,
;;      set by DI_LOAD) into RAM starting at ADDRHI:ADDRLO. The Read Sector command
;;      loads its whole buffer synchronously, so this can just drain FD_DATA in a
;;      tight loop - no DRQ/interrupt handling needed for a simple polling reader. ----
CMD_LOAD LDA #$80
	STA FD_STATUS
	LDY #0
CL_LOOP	LDA FD_DATA
	STA (ADDRLO),Y
	INY
	CPY #128
	BNE CL_LOOP
	RTS

;; ---- P: write one 128-byte sector from memory (ADDRHI:ADDRLO) to the selected
;;      drive at FD_TRACK/FD_SECTOR (set by DI_PUT). Mirrors CMD_LOAD exactly, just
;;      in the opposite direction - feeding FD_DATA drives the controller's Write
;;      Sector transfer, which flushes to the backing file on the 128th byte. ----
CMD_SAVE LDA #$A0
	STA FD_STATUS
	LDY #0
CP_LOOP	LDA (ADDRLO),Y
	STA FD_DATA
	INY
	CPY #128
	BNE CP_LOOP
	RTS

;; ---- bus fault handler (CPU.VECTOR_FAULT, $FFF8): entered via a pushed PC+PSR like a
;;      real interrupt, but there's no sense resuming the faulting instruction (it would
;;      just fault again), so this discards the pushed state and returns straight to the
;;      prompt loop instead of attempting RTI. Fixed at $1800 - NOT $1400 as originally
;;      chosen, which turned out to be too close: adding the S/F/L disk commands pushed
;;      the program's own tail past $1400, and crasm silently resumed emitting FAULT's
;;      bytes from $1400 anyway, overlapping (and corrupting) the end of CMD_LOAD rather
;;      than erroring. $1800 leaves real headroom (0x800 bytes) past the program's current
;;      end. Java-side harnesses (CPUMain, tests) wire the fault vector to this same fixed
;;      address independent of the program's actual size. ----
* = $1800
FAULT	PLA		;; discard the pushed PSR
	PLA		;; discard the pushed PCL
	PLA		;; discard the pushed PCH
	JSR HUH		;; reuse the same "?" + newline the unknown-command path prints
	JMP LOOP

;; No vectors defined here: unlike RTCInt.hex (loaded directly into basicROM's own
;; constructor, bypassing write-protection), BIOS.hex is loaded via CPU_CreateUtil.load()
;; - plain bus writes - and the vector range is ROM-protected. CPUMain and the test
;; harness set vectors separately via rom.setVectors()/setFaultVector().
