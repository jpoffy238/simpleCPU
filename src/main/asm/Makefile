.SUFFIXES: .hex .asm
# ALL:  JSR_TEST.hex JMP_ABS.hex JMP_IND.hex ADC_Test.hex Echo.hex RTCInt.hex LDA_IMM_TEST.hex JSR_TEST2.hex LDX_IMM_TEST1.hex ADC_DEC_mode.hex
	

install: $(patsubst %.asm,%.hex,$(wildcard *.asm))
	cp *.hex ../../test/resources
	cp Echo.hex ../../main/resources

clean: 
	rm *.hex *.lst

%.hex : %.asm
	crasm  $< -o $@ > $(@).lst

