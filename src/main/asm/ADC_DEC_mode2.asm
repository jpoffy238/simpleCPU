CPU 6502
OUTPUT HEX

* = $1000

	LDX	#$00 ;; use to
	SED		;; set BCD mode
	LDA	#$99
	ADC #$01  ;; Answer should be 00 with C = 1
	NOP
HLT	DB $3F


