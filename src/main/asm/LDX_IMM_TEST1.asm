
CPU 6502
OUTPUT HEX

* = $1000

START		LDX #$0A
			STX VALUE

HLT DB $3F
VALUE=$2000

