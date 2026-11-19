# Builds Z80 C programs with SDCC - the Z80/SDCC counterpart to the 6502
# side's own modules/cpu/6502/src/main/c/Makefile (cl65). Not wired into the
# Maven build, same manual-step convention that Makefile already uses for
# its own "apps" (ccp.bin, edit.bin, graphics.bin, hello.bin) and BDOS - see
# its own comment for why. Test_Z80HelloWorld/Test_Z80CCP skip themselves
# (Assumptions.assumeTrue) if their own .ihx hasn't been built yet, same
# pattern Test_CCP/Test_Edit use on the 6502 side.
#
# SDCC's own front end refuses to accept a hand-written .asm file directly
# on its command line ("file extension unsupported") - confirmed by hand:
# it only drives its own internal compile step end to end for .c inputs.
# Every .asm file here must be assembled separately with sdasz80 first;
# sdcc is then used only to compile a .c file and link the resulting .rel
# files (sdcc's own link step is just a thin driver over sdldz80 either
# way, same as its compile step is a driver over sdasz80 for a .c input).
#
# bios.h/bdos.h and hello.c/bdos.c/ccp.c/bios_util.c all live in
# ../../../c-shared, shared unchanged with the 6502/cc65 side - see
# bios.h/bdos.c's own comments for the real differences between the two
# (bios_video_init() and TRANSFER's address, both Z80-only/#ifdef'd).
#
# --code-loc only relocates where the compiled _CODE area (your own
# functions, including main) begins - it does NOT change where execution
# starts. SDCC's own crt0 relies on the classic Z80 convention that
# power-on starts at address 0 (a real reset vector, "JP $0100" into the
# real crt0 - SP init, BSS zeroing, then CALL _main - that this same .ihx
# always carries); boot any SDCC-built program with a crt0 at pc=0, not
# --code-loc's address (unlike hand-written Demo.asm, which has no crt0 of
# its own and really does start executing at its own `org`). Found the
# hard way - see Test_Z80HelloWorld's own comment. CODE_LOC here just
# keeps every SDCC "app"'s own code starting from the same familiar
# address as Demo.asm's `org $1000`, nothing more.
#
# BDOS is different: --no-std-crt0 strips the reset-vector/RST-trampoline/
# crt0 regions entirely (confirmed by hand: a --no-std-crt0 build's own
# .ihx holds nothing but the compiled function bytes at --code-loc) -
# required here since BDOS.ihx and whatever "app" needs it (ccp.ihx) get
# loaded into the SAME 64K address space at once; two full crt0s would
# collide at address 0 and $0100. Exactly mirrors why the 6502 side's own
# bdos.cfg excludes cc65's STARTUP segment (see bdos.c's own file
# comment) - same problem, same fix, different toolchain. BDOS is entered
# only through its own jump table (bdos_jumptable.asm, linked FIRST so its
# JPs land exactly at --code-loc $A000 - sdldz80 packs each linked file's
# _CODE content in command-line order within that one area), never run as
# "the" program, so it needs no SP init or crt0 of its own - Z80's SP is a
# real hardware register the CPU itself manages via CALL/RET, unlike
# cc65's own emulated software stack pointer, so (unlike the 6502 port)
# there is no extra "whoever calls BDOS first must init SP" gotcha to
# worry about here at all.

SDCC_BIN = /home/jpoffen/git/sdcc/sdcc-4.6.0/bin
SDCC = $(SDCC_BIN)/sdcc
SDASZ80 = $(SDCC_BIN)/sdasz80
CSHARED = ../../../../c-shared
CODE_LOC = 0x1000
BDOS_LOC = 0xA000

APPS = hello ccp

.PHONY: all install clean

all: bdos.ihx $(patsubst %,%.ihx,$(APPS))

bios_glue.rel: bios_glue.asm
	$(SDASZ80) -o bios_glue.rel bios_glue.asm

bdos_glue.rel: bdos_glue.asm
	$(SDASZ80) -o bdos_glue.rel bdos_glue.asm

bdos_jumptable.rel: bdos_jumptable.asm
	$(SDASZ80) -o bdos_jumptable.rel bdos_jumptable.asm

bios_util.rel: $(CSHARED)/bios_util.c $(CSHARED)/bios.h
	$(SDCC) -mz80 -c -I$(CSHARED) -o bios_util.rel $(CSHARED)/bios_util.c

bdos.rel: $(CSHARED)/bdos.c $(CSHARED)/bios.h
	$(SDCC) -mz80 -c -I$(CSHARED) -o bdos.rel $(CSHARED)/bdos.c

bdos.ihx: bdos_jumptable.rel bdos.rel bios_glue.rel
	$(SDCC) -mz80 --no-std-crt0 --code-loc $(BDOS_LOC) -o bdos.ihx bdos_jumptable.rel bdos.rel bios_glue.rel

hello.rel: $(CSHARED)/hello.c $(CSHARED)/bios.h
	$(SDCC) -mz80 -c -I$(CSHARED) -o hello.rel $(CSHARED)/hello.c

hello.ihx: hello.rel bios_glue.rel
	$(SDCC) -mz80 --code-loc $(CODE_LOC) -o hello.ihx hello.rel bios_glue.rel

ccp.rel: $(CSHARED)/ccp.c $(CSHARED)/bios.h $(CSHARED)/bdos.h
	$(SDCC) -mz80 -c -I$(CSHARED) -o ccp.rel $(CSHARED)/ccp.c

ccp.ihx: ccp.rel bios_glue.rel bdos_glue.rel bios_util.rel
	$(SDCC) -mz80 --code-loc $(CODE_LOC) -o ccp.ihx ccp.rel bios_glue.rel bdos_glue.rel bios_util.rel

install:
	@echo "Nothing installed yet - every .ihx here is a manual toolchain smoke test, not loaded by Java automatically (see this Makefile's own comment)."

clean:
	rm -f *.rel *.ihx *.lk *.lst *.map *.noi *.sym
