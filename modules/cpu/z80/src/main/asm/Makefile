.SUFFIXES: .z80 .asm

install: BIOS.z80 Demo.z80 DiskTest.z80 GraphicsTest.z80 ScrollTest.z80
	mkdir -p ../../test/resources ../../main/resources
	cp *.z80 ../../test/resources
	cp *.z80 ../../main/resources

clean:
	rm -f *.z80 *.lst

BIOS.z80: BIOS.asm
	z80-asm BIOS.asm 0xf000::BIOS.z80

Demo.z80: Demo.asm
	z80-asm Demo.asm 0x1000::Demo.z80

DiskTest.z80: DiskTest.asm
	z80-asm DiskTest.asm 0x1000::DiskTest.z80

GraphicsTest.z80: GraphicsTest.asm
	z80-asm GraphicsTest.asm 0x1000::GraphicsTest.z80

ScrollTest.z80: ScrollTest.asm
	z80-asm ScrollTest.asm 0x1000::ScrollTest.z80
