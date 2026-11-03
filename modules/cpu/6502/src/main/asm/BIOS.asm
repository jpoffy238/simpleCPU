;; Simple monitor BIOS: COMMAND ARGUMENTS style, read from and echoed to the console.
;;   D <addr>                    dump 256 bytes starting at addr, hexdump -C style
;;   W <addr> <byte> [byte...]   write one or more bytes starting at addr
CPU 6502
OUTPUT HEX

CONSOLE_DATA = $EC00
CONSOLE_STAT = $EC01
DATA_AVAILABLE = $01

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
;;      so both line endings are accepted here rather than just $0D. ----
READLINE LDY #0
RL_LOOP	JSR GETCH
	CMP #$0D
	BEQ RL_DONE
	CMP #$0A
	BEQ RL_DONE
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

;; ---- dispatch on the first non-space character of INBUF ----
DISPATCH LDA #0
	STA INPTR
	JSR SKIPSP
	LDY INPTR
	LDA INBUF,Y
	BEQ DI_DONE
	INY
	STY INPTR
	CMP #'D'
	BEQ DI_DUMP
	CMP #'W'
	BEQ DI_WRITE
	JSR HUH
	RTS
DI_DUMP	JSR SKIPSP
	JSR PARSEADDR
	JSR CMD_DUMP
	RTS
DI_WRITE JSR SKIPSP
	JSR PARSEADDR
	JSR CW_LOOP
DI_DONE	RTS

HUH	LDA #'?'
	JSR PUTCH
	JSR NEWLINE
	RTS

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

;; No vectors defined here: unlike RTCInt.hex (loaded directly into basicROM's own
;; constructor, bypassing write-protection), BIOS.hex is loaded via CPU_CreateUtil.load()
;; - plain bus writes - and the vector range is ROM-protected. Both CPUMain and the
;; test harness set vectors separately via rom.setVectors().
