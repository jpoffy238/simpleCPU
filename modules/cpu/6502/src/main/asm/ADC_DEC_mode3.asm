CPU 6502
OUTPUT HEX

* = $1000


	SED		;; set BCD mode
	SEC		;; Set carry flag see if it adds it correctly
	LDA	#$08
	ADC #$01  ;; Answer should be 00 with C = 1
	NOP
HLT	DB $3F


