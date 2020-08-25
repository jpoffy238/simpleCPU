.SUFFIXES: .hex .asm
ALL:  JSR_TEST.hex JMP_ABS.hex JMP_IND.hex ADC_Test.hex Echo.hex RTCInt.hex LDA_IMM_TEST.hex JSR_TEST2.hex
	

install: ALL
	cp *.hex ../../test/resources
	cp Echo.hex ../../main/resources

clean: 
	rm *.hex *.lst

%.hex : %.asm
	crasm  $< -o $@ > $(@).lst

