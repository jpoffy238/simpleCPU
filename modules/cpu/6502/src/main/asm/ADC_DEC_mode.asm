CPU 6502
OUTPUT HEX

* = $1000

		LDX	#$00 ;; use to
		SED		;; set BCD mode
		LDA	#$09
		ADC #$01
		NOP
HLT		DB $3F


