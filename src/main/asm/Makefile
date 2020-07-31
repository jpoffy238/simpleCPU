.SUFFIXES: .hex .asm
ALL:  JSR_TEST.hex JMP_ABS.hex JMP_IND.hex ADC_Test.hex
JSR_TEST.hex: JSR_TEST.asm
JMP_ABS.hex : JMP_ABS.asm

%.hex : %.asm
	crasm  $< -o $@ > $(@).lst

