


CPU 6502
OUTPUT HEX

* = $F000


START	SEI
		PHA  ;; save acc
		TYA ;; transfere to A to push on stack
		PHA  ;; push y on stack
		LDX #LEN  ;; setup bytes to transfere
LOOP	LDA RTC,X   ;; copy RTC to RAM
		STA RAM,X
        DEX
		BNE LOOP
		LDA RTC,X   ;; copy RTC to RAM
		STA RAM,X
		LDA #1
		STA FLAG	;; tell RTC_TEST the copy is done
		LDA #VIA_T1
		STA VIA_IFR	;; acknowledge the VIA's T1 flag - it's level-triggered,
				;; so leaving it set would re-fire the IRQ immediately on RTI
		PLA
		TAY
		PLA
		CLI
		RTI

RTC=$A000
RAM=$4000
FLAG=$4004	;; set to 1 after each RTC copy
LEN=3		;; u_time is 4 bytes (indexes 3..0)
VIA_IFR=$900D
VIA_T1=$40

NMI		RTI			;; no NMI sources yet: just return

;; Hardware vectors
* = $FFFA
		DW NMI		;; $FFFA NMI
		DW $1000	;; $FFFC RESET - test programs are loaded at $1000
		DW START	;; $FFFE IRQ
