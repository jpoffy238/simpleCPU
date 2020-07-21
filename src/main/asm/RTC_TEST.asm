
;; Test app for RTC	
PROCESSOR=6502
ORG=$1000
TIME=$4000
STL=$00
START=$1000

			CLI			;; ensure interrupts are handled.
LOOP:
		LDX #$1F      ;; Count down register
		JSR STRLN+ORG   ;; get length of string $00 terminated
		LDA #0                  ;; Load the value to compare null char
		CMP STL               ;; if String length is not 0 then halt - test done
		BNE HLT
		
WAIT:		NOP              ;; do nothing waste time
		DEX
		BNE WAIT	;; delay for some cycles
		BEQ LOOP        ;;  if wait is over check string length again

HLT: BYTE $3F

STRLN:
		NOP  		;; don't allow interrupts got cmpute string length
		PHA		;; Save current status of thing
		TYA 
		PHA
		TXA
		PHA
		LDY #0		;;
		LDA #0
		LDX #20         ;; max string size
LOOP1:		CMP TIME,Y	;; is this the end of the string
		BEQ EOS   ;; so if a null term char found - go to exit code
		INY            ;; did not found it so next char
		DEX           ;; one less char to check
		BNE LOOP1	;; not at end check next char
EOS:		STY STL      ;; String length in Y so store in it 0 page mem
		PLA              ;; restore registers
		TAX
		PLA
		TYA
		PLA

		RTS
		

