CPU 6502
OUTPUT HEX

* = $1000

COUT =$EC00

START   JSR PRT
WAIT    LDA COUT
        BNE WAIT
        STA COUT
        JMP START
        
PRT     LDX #$10
        LDY #0
        LDA PRPT,Y
        BEQ PRTE
        STA COUT
        INY
        DEX
        BNE PRT
        
PRTE    RTS        
        
PRPT ASC "> \0"
