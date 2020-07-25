.SUFFIXES: .hex .a
JSR_TEST.hex: JSR_TEST.a 

%.hex : %.a
	crasm  $< -o $@ > $(@).lst
