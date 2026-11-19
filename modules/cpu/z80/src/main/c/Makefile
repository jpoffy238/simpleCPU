# Builds Z80 C programs with SDCC - the Z80/SDCC counterpart to the 6502
# side's own modules/cpu/6502/src/main/c/Makefile (cl65). Not wired into the
# Maven build, same manual-step convention that Makefile already uses for
# its own "apps" (ccp.bin, edit.bin, graphics.bin, hello.bin) - see its own
# comment for why. Test_Z80HelloWorld skips itself (Assumptions.assumeTrue)
# if hello.ihx hasn't been built yet, same pattern Test_CCP/Test_Edit use on
# the 6502 side.
#
# SDCC's own front end refuses to accept a hand-written .asm file directly
# on its command line ("file extension unsupported") - confirmed by hand:
# it only drives its own internal compile step end to end for .c inputs.
# bios_glue.asm must be assembled separately with sdasz80 first; sdcc is
# then used only to compile hello.c and link the resulting .rel files
# (sdcc's own link step is just a thin driver over sdldz80 either way, same
# as its compile step is a driver over sdasz80 for a .c input).
#
# bios.h (and hello.c itself) live in ../../../c-shared, shared unchanged
# with the 6502/cc65 side - see that header's own comment for the one real
# difference between the two (bios_video_init(), Z80-only).
#
# --code-loc only relocates where the compiled _CODE area (your own
# functions, including main) begins - it does NOT change where execution
# starts. SDCC's own crt0 relies on the classic Z80 convention that
# power-on starts at address 0 (a real reset vector, "JP $0100" into the
# real crt0 - SP init, BSS zeroing, then CALL _main - that this same .ihx
# always carries); boot any SDCC-built program at pc=0, not --code-loc's
# address (unlike hand-written Demo.asm, which has no crt0 of its own and
# really does start executing at its own `org`). Found the hard way - see
# Test_Z80HelloWorld's own comment. CODE_LOC here just keeps every SDCC
# program's own code starting from the same familiar address as Demo.asm's
# `org $1000`, nothing more.

SDCC_BIN = /home/jpoffen/git/sdcc/sdcc-4.6.0/bin
SDCC = $(SDCC_BIN)/sdcc
SDASZ80 = $(SDCC_BIN)/sdasz80
CSHARED = ../../../../c-shared
CODE_LOC = 0x1000

APPS = hello

.PHONY: all install clean

all: $(patsubst %,%.ihx,$(APPS))

bios_glue.rel: bios_glue.asm
	$(SDASZ80) -o bios_glue.rel bios_glue.asm

hello.rel: $(CSHARED)/hello.c $(CSHARED)/bios.h
	$(SDCC) -mz80 -c -I$(CSHARED) -o hello.rel $(CSHARED)/hello.c

hello.ihx: hello.rel bios_glue.rel
	$(SDCC) -mz80 --code-loc $(CODE_LOC) -o hello.ihx hello.rel bios_glue.rel

install:
	@echo "Nothing installed yet - hello.ihx is a manual toolchain smoke test, not loaded by Java automatically (see this Makefile's own comment)."

clean:
	rm -f hello.rel bios_glue.rel hello.ihx *.lk *.lst *.map *.noi *.sym
