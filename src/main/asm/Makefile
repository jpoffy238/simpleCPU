.SUFFIXES: .hex .asm
JSR_TEST.hex: JSR_TEST.asm

%.hex : %.asm
	crasm  $< -o $@ > $(@).lst
