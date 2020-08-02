.SUFFIXES: .hex .asm
ALL:  JSR_TEST.hex JMP_ABS.hex JMP_IND.hex ADC_Test.hex Echo.hex RTCInt.hex

%.hex : %.asm
	crasm  $< -o $@ > $(@).lst

