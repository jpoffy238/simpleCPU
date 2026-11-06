# Builds BDOS (the ROM) and every "app" (.c file meant to be loaded as a
# regular file and run - CCP.COM today, whatever else later) with cl65.
#
# BDOS gets its own rule: a different linker config (bdos.cfg, ROM-origin
# $A000 rather than RAM-origin $0300) and a different set of files to link
# (bdos_jumptable.s instead of a generic app's own code calling through
# bdos_glue.s). It's also the one binary Java actually loads directly
# (AbstractMemoryLayer.loadBinary() - see bdos.cfg's comment for why this
# needs a raw-binary load rather than the Intel-hex path BIOS.hex/
# MONITOR.hex use), so "install" copies it into the resource directories
# the same way the asm Makefile already does for those two.
#
# Every app links against both bios_glue.s and bdos_glue.s unconditionally,
# even one like hello.c that calls neither - cl65 includes an explicitly
# listed input file's code wholesale, unlike library linking, so this costs
# a handful of unused bytes per app in exchange for one simple rule instead
# of a per-app list of which glue files it happens to need.
#
# Apps aren't installed anywhere by this Makefile - unlike BDOS, they're
# not loaded by Java at all. They become CCP.COM (or whatever) by hand, via
# CpmDiskTool's "put" subcommand onto a disk image:
#   mvn -pl modules/Disk exec:java -Dexec.mainClass=com.mj.Disk.CpmDiskTool \
#       -Dexec.args="put disk0.img ccp.bin CCP.COM"

CC65 = cl65
CFLAGS = -O -t none

APPS = hello ccp

.PHONY: all install clean

all: bdos.bin $(patsubst %,%.bin,$(APPS))

bdos.bin: bdos.c bdos_jumptable.s bios_glue.s bdos.cfg bios.h
	$(CC65) $(CFLAGS) -C bdos.cfg -m bdos.map -o bdos.bin bdos.c bdos_jumptable.s bios_glue.s

%.bin: %.c bios_glue.s bdos_glue.s simplecpu.cfg bios.h bdos.h
	$(CC65) $(CFLAGS) -C simplecpu.cfg -m $*.map -o $@ $< bios_glue.s bdos_glue.s

install: bdos.bin
	cp bdos.bin ../../main/resources
	cp bdos.bin ../../test/resources

clean:
	rm -f *.bin *.map
