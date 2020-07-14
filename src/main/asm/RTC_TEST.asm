
;; Test app for RTC	
PROCESSOR=6502
ORG=$1000
TIME=$4000
STL=$00
START=$1000
		
LOOP:		LDX #$7F
		JSR STRLN+ORG
		LDA #0
		CMP STL
		BNE HLT
		
WAIT:		NOP
		DEX
		BNE WAIT	;; delay for some cycles
		BEQ LOOP        ;;  if wait is over check string length again

HLT: BYTE $3F
STRLN:		PHA		;; Save current status of thing
		TYA 
		PHA
		TXA
		PHA
		LDY #0		;;
		LDA #0
		LDX #20         ;; max string size
LOOP1:		CMP TIME,Y	;; is this the end of the string
		BEQ EOS
		INY
		DEX
		BNE LOOP1	;; not at end check next char
EOS:		STY STL
		PLA
		TAX
		PLA
		TYA
		PLA
		RTS
		

