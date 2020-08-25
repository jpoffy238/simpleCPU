;; Test app for BCS	Branch if carry flag set
CPU 6502
OUTPUT HEX

* = $1000

VALUE=$2000
RESULT=$2001


		LDA #50     ;; lOAD 10 into acc
		ADC #25    ;; This should set cary flag
		STA VALUE    ;; store new value in value.
		BCS SET       ;; If carry set call set subroutine
		JSR CNS ;; else call carry clear subroutine
		JMP HLT  ;; stop

SET		JSR CST
		JMP HLT

CLR		JSR CNS
		JMP HLT
		NOP
HLT DB $3F

CST		LDA #'C'
		STA RESULT
		RTS
		NOP
		NOP
HLT2 DB $3F

CNS		LDA #'N'
		STA RESULT
		RTS
		NOP
		NOP
HLT3 DB $3F
