PROCESSOR=6502
ORG=$1000
VALUE=$2000

START=$1000
			LDA #$0A     ;; lOAD 10 into acc
			JSR ADDONE+START  ;; Just to sub to add 1
			STA VALUE    ;; store new value in value.
			NOP
			NOP
HLT: BYTE $3F

ADDONE:
		ADC #$01
		RTS
		NOP
		NOP
HLT2: BYTE $3F

