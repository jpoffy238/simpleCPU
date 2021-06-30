CPU 6502
OUTPUT HEX

* = $1000


	SED		;; set BCD mode
	CLC		;; Clear carry flag see if it adds it correctly
	LDA	#$99
	ADC #$99  ;; Answer should be 98 with C = 1
	NOP
HLT	DB $3F


