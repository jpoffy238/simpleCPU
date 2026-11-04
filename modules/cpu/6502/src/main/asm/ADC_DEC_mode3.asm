CPU 6502
OUTPUT HEX

* = $1000


	SED		;; set BCD mode
	SEC		;; Set carry flag see if it adds it correctly
	LDA	#$08
	ADC #$01  ;; 08 + 01 + carry(1) = 10 in BCD, C = 0
	NOP
HLT	DB $3F


