PROCESSOR=6502
ORG=$1000
VALUE=$2000

START=$1000
			LDX #$0A     ;; Setup to cal ADDONE 10 times
			LDA #$0A     ;; lOAD 10 into acc
LOOP:
			JSR ADDONE+START  ;; Just to sub to add 1
			STA VALUE,X    ;; store new value in value.
			DEX
			BNE LOOP
			NOP
			NOP
HLT: BYTE $3F

ADDONE:
		ADC #$01
		RTS
		NOP
		NOP
HLT2: BYTE $3F

