PROCESSOR .6502
ORG=$F000



START:				SEI
				PHA  ;; save acc
				TYA ;; transfere to A to push on stack
				PHA  ;; push y on stack
				LDX #LEN  ;; setup 20 bytes to transfere
LOOPA:				LDA RTC,X   ;; copy RTC to RAM
				STA RAM,X
				DEX
				BNE LOOPA
				LDA RTC,X   ;; copy RTC to RAM
				STA RAM,X
				PLA
				TAY
				PLA
				CLI
				RTI
END:
RTC=$A000
RAM=$4000
LEN=20
;	
;		;
