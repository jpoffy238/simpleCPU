CPU 6502
OUTPUT HEX

* = $1000
		LDX #$0A     ;; Setup to cal ADDONE 10 times
		LDA #$0A     ;; lOAD 10 into acc
LOOP	JSR ADDONE  ;; Just to sub to add 1
		STA VALUE,X    ;; store new value in value.
		DEX
		BNE LOOP
		NOP
		NOP
HLT 	DB $3F

ADDONE	ADC #$01
		RTS
		NOP
		NOP
HLT2	DB  $3F

* = $2000
VALUE 	DB $00

