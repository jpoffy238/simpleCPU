PROCESSOR .6502
ORG=$F000



START:
	PHA
	TYA
	PHA
	LDY LEN
LOOPA:
	LDA (RTC),Y
	STA (RAM),Y
	DEY
	BNE LOOPA
	LDA (RTC),Y
	STA (RAM),Y
	PLA
	TAY
	PLA
	RTI
END:
RTC: DW $a000
RAM: DW $4000
LEN: BYTE 20
;	
;		;
