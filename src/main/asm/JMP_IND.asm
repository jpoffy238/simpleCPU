CPU 6502
OUTPUT HEX

* = $1000
	SEI
	LDA #$0A     ;; lOAD 10 into acc
	JMP (TAD)   ;; Just to sub to add 1
RT	STA VALUE    ;; store new value in value.
	NOP
	JMP (RDH)	
HLT DB $3F

ADDONE	ADC #$01
	JMP (TRT)
	NOP
	NOP
HLT2 DB $3F

VALUE = $2000
TAD DW ADDONE
TRT DW RT
RDH DW HLT2

