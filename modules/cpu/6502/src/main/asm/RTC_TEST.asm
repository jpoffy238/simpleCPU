;; Test app for RTC
;; Spins until the interrupt handler (RTCInt) has copied the RTC registers to TIME,
;; then halts.  STRLN measures the length of the zero-terminated string at TIME and
;; leaves it in STL; the loop ends as soon as the string is non-empty.
CPU 6502
OUTPUT HEX

STL  = $00		;; zero page: string length computed by STRLN
TIME = $4000		;; RTCInt copies the RTC registers (a text date/time) here

* = $1000

		CLI			;; ensure interrupts are handled.
LOOP	LDX #$1F		;; Count down register
		JSR STRLN		;; get length of string $00 terminated
		LDA #0			;; Load the value to compare null char
		CMP STL			;; if String length is not 0 then halt - test done
		BNE HLT

WAIT	NOP			;; do nothing waste time
		DEX
		BNE WAIT		;; delay for some cycles
		BEQ LOOP		;; if wait is over check string length again

HLT		DB $3F

STRLN	SEI			;; don't allow interrupts while computing the string length
		PHA			;; save A, Y and X
		TYA
		PHA
		TXA
		PHA
		LDY #0
		LDA #0
		LDX #20			;; max string size
LOOP1	CMP TIME,Y		;; is this the end of the string
		BEQ EOS			;; null terminator found - go to exit code
		INY			;; did not find it so next char
		DEX			;; one less char to check
		BNE LOOP1		;; not at end, check next char
EOS		STY STL			;; string length is in Y, so store it
		PLA			;; restore X, Y and A
		TAX
		PLA
		TAY
		PLA
		CLI			;; allow interrupts
		RTS
