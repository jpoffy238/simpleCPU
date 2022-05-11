;;PROCESSOR=6502
;;ORG=$1000
;;VALUE=$2000
CPU 6502
OUTPUT HEX

* = $1000
	LDA #$0A     ;; lOAD 10 into acc
	JSR ADDONE   ;; Just to sub to add 1
	STA VALUE    ;; store new value in value.
	NOP
	NOP
HLT DB $3F

ADDONE	ADC #$01
	RTS
	NOP
	NOP
HLT2 DB $3F

VALUE = $2000

