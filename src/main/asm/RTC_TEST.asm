
;; Test app for BCS	Branch if carry flag set
PROCESSOR=6502
ORG=$1000

START=$1000
				LDA #0
				STA $4000
	
LOOP:				LDX #$7F
WAIT:				NOP
				DEX
				BNE WAIT	;; delay for some cycles
				CMP $4000       ;; Compare to see if value changed
				BEQ LOOP        ;; if not loop some more
HLT: BYTE $3F
