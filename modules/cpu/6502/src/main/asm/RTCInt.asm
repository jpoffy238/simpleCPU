


CPU 6502
OUTPUT HEX

* = $F000


START	SEI
		PHA  ;; save acc
		TYA ;; transfere to A to push on stack
		PHA  ;; push y on stack
		LDX #LEN  ;; setup 20 bytes to transfere
LOOP	LDA RTC,X   ;; copy RTC to RAM
		STA RAM,X
        DEX
		BNE LOOP
		LDA RTC,X   ;; copy RTC to RAM
		STA RAM,X
		PLA
		TAY
		PLA
		CLI
		RTI

RTC=$A000
RAM=$4000
LEN=20

NMI		RTI			;; no NMI sources yet: just return

;; Hardware vectors
* = $FFFA
		DW NMI		;; $FFFA NMI
		DW $1000	;; $FFFC RESET - test programs are loaded at $1000
		DW START	;; $FFFE IRQ
