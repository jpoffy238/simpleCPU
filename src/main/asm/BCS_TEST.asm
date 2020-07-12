;; Test app for BCS	Branch if carry flag set
PROCESSOR=6502
ORG=$1000
VALUE=$2000
RESULT=$2001

START=$1000
			LDA #100     ;; lOAD 10 into acc
			ADC #175    ;; This should set cary flag
			STA VALUE    ;; store new value in value.
			BCS SET       ;; If carry set call set subroutine
			JSR CNS+ORG ;; else call carry clear subroutine
			JMP HLT+ORG  ;; stop

SET:		JSR CST+ORG
			JMP HLT+ORG

CLR:		JSR CNS+ORG
			JMP HLT+ORG
			NOP
HLT: BYTE $3F

CST:		LDA #'C'
			STA RESULT
			RTS
			NOP
			NOP
HLT2: BYTE $3F

CNS:		LDA #'N'
			STA RESULT
			RTS
			NOP
			NOP
HLT3: BYTE $3F
