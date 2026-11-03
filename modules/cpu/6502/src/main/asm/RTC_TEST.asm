;; Test app for RTC
;; Spins until the interrupt handler (RTCInt) has copied the RTC's raw u_time
;; to TIME and set FLAG, then halts.
CPU 6502
OUTPUT HEX

TIME = $4000		;; RTCInt copies the 4-byte big-endian u_time here
FLAG = $4004		;; RTCInt sets this to 1 after each copy
VIA_IER = $900E

* = $1000

		LDA #$C0		;; set-mode (bit7) + T1 (bit6): enable the RTC tick interrupt
		STA VIA_IER
		CLI			;; ensure interrupts are handled.
LOOP	LDX #$1F		;; Count down register
		LDA FLAG
		BNE HLT			;; FLAG set - RTCInt has run, test done

WAIT	NOP			;; do nothing waste time
		DEX
		BNE WAIT		;; delay for some cycles
		BEQ LOOP		;; if wait is over check FLAG again

HLT		DB $3F
