CPU 6502
OUTPUT HEX

* = $1000

COUT =$EC00

START   JSR PRT
WAIT    LDA COUT
        BEQ WAIT
        STA COUT
        JMP START
        
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
