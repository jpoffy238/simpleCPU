CPU 6502
OUTPUT HEX
PAGE 100,150

* = $1000

COUT =$EC00

START   JSR PRT		;; print prompt
WAIT    LDA COUT 	;; grab char from console - it though echos char.
			;; currently it is setup to wait until a charactor
			;; is typed but should read the status flag as to if
			;; there is a character ready else return.
        BEQ WAIT	;; if no charactor - then wait for one.
        STA COUT
        CMP	#$0a
        BEQ START
        JMP WAIT
        
PRT     LDX #$10
        LDY #0
LOOP    LDA PRPT,Y
        BEQ PRTE
        STA COUT
        INY
        DEX
        BNE LOOP
        
PRTE    RTS        
        
PRPT ASC "> \0"
