PROCESSOR=6502
ORG=$1000

START=$1000
			LDA #$0A
			STA VALUE

HTL: BYTE $3F
VALUE=$2000

