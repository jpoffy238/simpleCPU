# simpleCPU Architecture

Two Java-emulated CP/M-era CPUs - a validated NMOS 6502 core and a Z80 core
built up the same way after it - sharing a CPU-agnostic device/bus layer, a
disk subsystem, a CRTC-style video device, and (since both run the same
CP/M-flavored DOS) a shared C codebase for BDOS/CCP/BIOS-primitive glue.
This document describes how all of that fits together as of 2026-11-20.

## Module layout

```
modules/
  Devices/        CPU-agnostic bus: Device/PBus/DeviceBus/AddressRange,
                   InterruptLine, VIA6522 (6502-only hardware), RTC,
                   InterruptVectorSource (Z80 IM2 support), device exceptions.
  Disk/            FloppyController (WD179x-style, shared by both CPUs) and
                   file-backed virtual disks (VirtualDisk/FileVirtualDisk/
                   IBM3740); CpmFileSystem/CpmDiskTool (host-side CP/M
                   filesystem reference); Imd/ (reads real period .IMD CP/M
                   disk images).
  Video/           VideoController - a CRTC-style display controller, the
                   sole console on both CPUs (text + 320x200 1bpp graphics).
  cpu/6502/        The 6502 core (com.mj.cpu001.CPU), the interactive
                   CPUMain entry point, and the 6502-specific firmware
                   (src/main/asm: BIOS.asm/MONITOR.asm, hand-assembled via
                   crasm).
  cpu/z80/         The Z80 core (com.mj.z80.Z80CPU), the interactive Z80Main
                   entry point, and the Z80-specific firmware (src/main/asm:
                   BIOS.z80, hand-assembled via the z80-asm toolchain).
  cpu/c-shared/    Plain C source (not a Maven module) shared unchanged
                   between both CPUs: bios.h/bdos.h, bios_util.c, bdos.c,
                   ccp.c, edit.c, graphics.c, hello.c. Compiled by cc65 for
                   the 6502 side, SDCC for the Z80 side.
```

`Devices`/`Disk`/`Video` exist as their own modules specifically so a second
CPU core could reuse them without duplicating anything - and that's exactly
what happened: `cpu/z80` depends on all three unchanged, the same way
`cpu/6502` does. Neither CPU module depends on the other.

## The bus

Every device (`Device` interface) sits behind a shared `PBus` (`DeviceBus`
is the concrete implementation). A device registers an `AddressRange`;
`DeviceBus.read()/write()` linearly scans registered ranges to find the
device that `contains()` a given address and delegates to it. There's no
bank switching or address decoding beyond this - a read/write to an address
with no device behind it throws `DeviceUnavailable`, which the 6502's
`CPU.run()` treats as a recoverable bus fault (see below), not a crash.

**Two independent buses on the Z80 side.** The 6502 has one unified address
space; the Z80 has a real `/IORQ` vs `/MREQ` pin distinction, modeled as two
separate `DeviceBus` instances - `memoryBus` for every ordinary instruction,
`portBus` for `IN`/`OUT` alone. `Z80CPU` holds both. A port-space device
(`FloppyController`, `VideoController`'s register set, `RTC`,
`VectoredInterruptController`) uses the exact same `Device`/`AddressRange`
machinery as a memory-mapped one - just registered on the port bus with a
narrow range instead of the full 64K memory one. No changes were needed in
`modules/Devices` itself to support this.

**`InterruptLine`** (`modules/Devices`, one method: `raiseInterrupt()`) is
what lets a CPU-agnostic device like `FloppyController` depend on "something
it can interrupt" without depending on *which* interrupt controller a given
machine uses - the 6502 side passes a lambda wrapping a `VIA6522` call (real
period hardware for that CPU); the Z80 side passes one wrapping
`portBus.raiseInterupt()` directly, or (for RTC) a
`VectoredInterruptController`'s `requestInterrupt()` - more period-correct
for a Z80 machine, which has no business needing a 6502-only VIA just to
share this class.

## 6502 CPU core

`com.mj.cpu001.CPU` is the 6502 itself: registers, flags, the fetch/decode/
execute loop (`run()`), and the stack (hardwired to `$0100-$01FF`, as on real
hardware). Most instructions are table-driven (`Firmware.Table`);
execution-flow instructions (`JMP`/`JSR`/`RTS`/`RTI`/`BRK`/branches) and the
project-specific `MIH` ("maskable interrupt handler" injection) and `HLT`
instructions are hand-written in `Firmware.ExecutionFlow`. `HLT` is a
project-specific instruction (not real 6502) used by unit tests to signal
"this program is done" - it halts the whole emulated CPU and is never meant
to be used by real firmware. `CPU` itself `extends Thread`, running on its
own thread so Swing's event dispatch thread (video) stays responsive.

### Interrupts and the bus-fault vector

`fetchInstruction()` checks for a pending interrupt before every opcode
fetch, injecting a synthetic `MIH` instruction that reads the right vector
and transfers control, same as real 6502 hardware timing. Three vector pairs
exist: NMI (`$FFFA`), RESET (`$FFFC`), IRQ (`$FFFE`) - standard 6502 - plus
one project-specific extension:

- **`VECTOR_FAULT` ($FFF8)**: reading or writing an address with no device
  behind it throws `DeviceUnavailable`. Rather than halting the emulated
  machine outright, `CPU.run()` pushes PC+status (like a real interrupt) and
  jumps through this vector, giving firmware a chance to recover. The
  monitor ROM's fault handler discards the pushed state and returns straight
  to the prompt rather than attempting `RTI`, since resuming the faulting
  instruction would just fault again.

### Interrupt aggregation (VIA6522)

Every 6502-side device that wants to interrupt the CPU goes through a
`VIA6522` (real MOS 6522 register semantics for IFR/IER) rather than
touching the bus's shared IRQ line directly - the VIA is the single point
that aggregates per-device interrupt sources onto that one line, same as
real hardware. Only the interrupt subsystem (IFR/IER) is functional; ports,
timers, and the shift register are addressed but inert. Current sources:
`RTC` via `FLAG_T1`, `FloppyController` via `FLAG_CB1`.

## 6502 memory map

The address space as `CPUMain` wires it up (every test fixture that boots
real firmware - `Test_BIOS`, `Test_CCP`, `Test_Edit` - duplicates this same
layout by hand, since there's no shared "6502 machine" constructor the way
`Z80VideoDiskMachine` is on the Z80 side):

```
$0000-$00FF   Zero page (monitor-private vars + the BIOS-call ABI's
              ADDRLO/ADDRHI parameter-passing slot)
$0100-$01FF   Stack (fixed by 6502 hardware)
$0200-$02FF   INBUF (the monitor's command-line buffer)
$0300-$7FFF   BOOTADDR onward - where B loads CCP.COM's content; any file
              beyond a page or so runs well past the $0300-$03FF page into
              the free RAM beyond it
$8000-$81FF   BDOS's own working RAM (directory-scan position, "currently
              open"/"currently being written" file state, block-allocation
              bitmap, sector buffer) - plain basicMemory, not part of the
              ROM chip below, since BDOS's C code writes to it at runtime
$8200-$9024   Unmapped except the MMIO cluster below
$9000-$900F   VIA6522
$9010-$9014   FloppyController (status/command, track, sector, data,
              drive-select)
$9020-$9024   VideoController's register set (CRTC Address/Data, keyboard
              data/status, Mode Control)
$9025-$9028   RTC (big-endian Unix-epoch-seconds, read-only, 4 bytes)
$9029-$9FFF   Unmapped ($9100 is Test_BIOS's fault-recovery probe address)
$A000-$B7FF   ROM chip 3: BDOS
$B800-$BFFF   VideoController's character RAM (text mode)
$C000-$DFFF   VideoController's graphics RAM (320x200 1bpp, MODE_GRAPHICS)
$F000-$F7FF   ROM chip 1: BIOS primitives
$F800-$FFFF   ROM chip 2: Monitor, including the hardware + fault vectors
```

Zero page, stack, and RAM up to `$7FFF` are plain `basicMemory`. `RTC`'s own
address here deliberately differs from the `$A000` it uses in the
`CPU_CreateUtil` test-scratch bus (see RTC below) - that address would
collide with BDOS ROM in the real machine.

## Firmware: a three-ROM BIOS/monitor/BDOS split

The firmware is split into three ROM chips (three `basicROM` instances),
mirroring real systems that had distinct BIOS, monitor/BASIC, and (in
spirit) BDOS ROM sockets - though unlike the first two, BDOS is cc65-
compiled C, not hand assembly.

### BIOS.asm - primitives ROM ($F000-$F7FF)

Raw hardware access only - console, disk, graphics, and RTC I/O - no
command parsing or filesystem concepts. Exposes a fixed, frozen jump table
at `$F000`:

| Address | Routine             | Calling convention |
|---------|----------------------|---------------------|
| `$F000` | `BIOS_PUTCH`         | A = char to print |
| `$F003` | `BIOS_GETCH`         | blocks; returns A = char |
| `$F006` | `BIOS_SELECT`        | A = drive 0-3 |
| `$F009` | `BIOS_READ_SECTOR`   | A = track, X = sector, ADDRLO/ADDRHI = dest |
| `$F00C` | `BIOS_WRITE_SECTOR`  | A = track, X = sector, ADDRLO/ADDRHI = src |
| `$F00F` | `BIOS_FORMAT_TRACK`  | A = track, X = fill byte (one track only) |
| `$F012` | `BIOS_RESTORE`       | no parameters - homes the selected drive to track 0 |
| `$F015` | `BIOS_VIDEO_INIT`    | no parameters - one-time CRTC setup, clears the screen |
| `$F018` | `BIOS_GOTOXY`        | A = row (0-23), X = col (0-79) |
| `$F01B` | `BIOS_GRAPHICS_MODE` | A = MODE_TEXT or MODE_GRAPHICS |
| `$F01E` | `BIOS_SET_PIXEL`     | A = row (0-199), X = on/off, zero-page ADDRLO/ADDRHI = column (0-319), staged beforehand |
| `$F021` | `BIOS_GET_TIME`      | no parameters; zero-page ADDRLO/ADDRHI = destination 4-byte buffer - copies RTC's counter there as a little-endian `unsigned long` |

Each slot is a `JMP` to the real implementation below it - moving an
implementation is always safe as long as its slot still jumps to the right
place. `MONITOR.asm` and `bios_glue.s` are assembled completely separately
from `BIOS.asm` (no shared symbol table), so both hardcode these same
addresses as constants - a frozen ABI that must be kept in sync by hand if
anything here ever moves.

### MONITOR.asm - monitor ROM ($F800-$FFFF)

The `COMMAND ARGUMENTS`-style command interpreter, and the thing the reset
vector actually lands in. Never touches hardware registers directly - only
the jump table above. Also owns the hardware vector table (`$FFFA-$FFFF`)
and the fault vector (`$FFF8`), embedded directly via `DW` directives, since
reset should land in the monitor and fault recovery ("print `?`, return to
the prompt") is monitor policy, not a hardware primitive.

Commands:

| Command | Syntax | Effect |
|---|---|---|
| `D` | `D <addr>` | dump 256 bytes, hexdump -C style |
| `W` | `W <addr> <byte>...` | write one or more bytes |
| `H` | `H` | list commands |
| `E` | `E <addr>` | execute (JSR); routine should end in RTS |
| `S` | `S <drive>` | select floppy drive 0-3 |
| `F` | `F [fill]` | format every track on the selected drive |
| `L` | `L <track> <sector> <addr>` | load one sector into memory |
| `P` | `P <track> <sector> <addr>` | write one sector from memory to disk |
| `B` | `B` | boot: find `CCP.COM` via BDOS, load it to `BOOTADDR`, and JSR there |

`B` asks BDOS to find a file named `CCP.COM` (`BDOS_FIND_FILE`), then
repeatedly asks for its next 128-byte record (`BDOS_READ_RECORD`), appending
each one at `BOOTADDR` onward - real CP/M's own cold-loader mechanism, a
file in the directory rather than a fixed boot area. If `CCP.COM` isn't
found, `B` prints `?` and returns to the prompt.

**Self-modifying code and real write protection.** `E`'s implementation has
no way to `JSR` to a runtime-computed address (6502 `JSR` only takes a
literal operand), so it writes a fresh `JSR <addr>` instruction into a small
scratch area in zero page (writable RAM, not ROM) before calling it - the
classic monitor-ROM self-modifying-code technique.

**Branch-range safety.** `DISPATCH`'s command-letter comparisons branch to
small, nearby `JMP` stubs rather than branching to the real handlers
directly, since a direct `BEQ` is capped at a single signed byte (-128..127)
and kept getting exceeded as commands were added.

### BDOS - filesystem ROM ($A000-$B7FF)

The CP/M filesystem logic (directory scan, find file by name, read/write a
file's content record by record, rename, run a file as a program) - see CP/M
filesystem below for the on-disk format itself. Unlike BIOS.asm/MONITOR.asm,
this is C (`bdos.c`, `modules/cpu/c-shared`) - compiled by cc65 for the 6502
build, SDCC for the Z80 build, identically except two small `#ifdef`s (see
c-shared below). Exposes its own fixed, frozen jump table at `$A000`
(`bdos_jumptable.s` for 6502, `bdos_jumptable.asm` for Z80 - same addresses
on both CPUs, for easy cross-CPU comparison):

| Address | Routine | Calling convention |
|---------|---------|---------------------|
| `$A000` | `BDOS_REWIND_DIR` | no parameters - resets the directory scan position |
| `$A003` | `BDOS_NEXT_ENTRY` | ADDRLO/ADDRHI = 32-byte dest; returns A = 1 found / 0 done |
| `$A006` | `BDOS_FIND_FILE` | ADDRLO/ADDRHI = filename (null-terminated); returns A = 1 found / 0 not found |
| `$A009` | `BDOS_READ_RECORD` | ADDRLO/ADDRHI = 128-byte dest; returns A = 1 read / 0 done |
| `$A00C` | `BDOS_DELETE_FILE` | ADDRLO/ADDRHI = filename (null-terminated); returns A = 1 deleted / 0 not found |
| `$A00F` | `BDOS_CREATE_FILE` | ADDRLO/ADDRHI = filename (null-terminated); returns A = 1 created / 0 (exists, or directory full) |
| `$A012` | `BDOS_WRITE_RECORD` | ADDRLO/ADDRHI = 128-byte source; returns A = 1 written / 0 (directory or disk full) |
| `$A015` | `BDOS_ENTRY_SIZE` | ADDRLO/ADDRHI = the entry `BDOS_NEXT_ENTRY` just filled; returns `unsigned int` total record count across every extent |
| `$A018` | `BDOS_RENAME_FILE` | ADDRLO/ADDRHI = "NEWNAME\0OLDNAME\0"; returns A = 1 renamed / 0 not found |
| `$A01B` | `BDOS_RUN_FILE` | ADDRLO/ADDRHI = filename; loads it to `BOOTADDR` and jumps in - does **not** return normally if found (see `bdos.c`'s own comment) |
| `$A01E` | `BDOS_FREE_BLOCKS` | no parameters; returns `unsigned int` free block count |

Every primitive reuses `ADDRLO`/`ADDRHI` (the same pair `BIOS_READ_SECTOR`/
`BIOS_WRITE_SECTOR` already use) rather than adding a second pointer ABI.
Most return `unsigned char` (1/0) via the standard single-byte A return;
`BDOS_ENTRY_SIZE`/`BDOS_FREE_BLOCKS` return `unsigned int` via the standard
16-bit return (cc65: A low/X high; SDCC: HL).

**Multi-extent files** (see CP/M filesystem below for what an "extent" is)
are transparent to a caller of any of these primitives - `BDOS_READ_RECORD`
and `BDOS_WRITE_RECORD` cross from one extent into the next on their own.
Each direction keeps only *one* extent's state in memory at a time (16 block
pointers), not every extent up front.

**`BDOS_RUN_FILE`** (CP/M's "type a name to run it") loads a named file to
`BOOTADDR` and jumps in; when the loaded program returns, BDOS reloads
`CCP.COM` fresh (real CP/M's "warm boot") rather than returning to a caller
whose own code may no longer exist in memory. On the Z80 side this needed a
dedicated minimal crt0 (`cpm_crt0.asm`, just `call _main / ret`, no SP reset,
no BSS zeroing) rather than SDCC's own default one - see the Z80 section
below.

**No crt0 for BDOS itself**, on either CPU - nothing ever calls BDOS's
reset-style entry point, only the jump table's individual slots, so the
usual startup sequence (init stack pointer, zero BSS, call `main()`) never
runs. On the 6502 side this meant `MONITOR.asm`'s `DI_BOOT` has to
initialize cc65's software stack pointer once, at the very first BDOS call
ever, before any program's own crt0 has run (confirmed the hard way: with
`sp` garbage, BDOS's own internal division/modulo calls - which touch `sp`
indirectly via cc65's runtime helpers - corrupted ROM-adjacent memory). The
Z80 side has no equivalent gotcha: Z80's SP is a real hardware register the
CPU manages via `CALL`/`RET`, not an emulated software stack pointer.

**Two memory regions, not one** (6502 side). `bdos.cfg` (the linker config)
splits BDOS's code (the jump table plus `bdos.c`'s compiled functions - what
`cl65` produces as `bdos.bin`) from its working state (directory-scan
position, open-file block lists, sector buffer - plain C statics written at
runtime). The first is real, write-protected ROM at `$A000-$B7FF`; the
second is a separate `$8000-$81FF` RAM region with no corresponding bytes in
`bdos.bin` at all.

## Disk subsystem

`FloppyController` (`modules/Disk`, shared unchanged by both CPUs) is a
WD179x-style register set:

```
+0  Status (read) / Command (write)
+1  Track
+2  Sector
+3  Data
+4  Drive Select (0-7 - a real 3-line drive-select field; only as many
    drives as attachDrive() actually wires up report ready)
```

Commands (upper nibble of the command byte): `$0x` Restore, `$1x` Seek,
`$8x` Read Sector, `$Ax` Write Sector, `$Fx` Format Track. `IBM3740` is the
only `FloppyDevice` implementation so far - 1 head, 76 tracks (0-indexed),
26 sectors/track (1-indexed, matching the real IBM 3740's Rec# field),
128-byte sectors. Only the sector payload is emulated - gap/sync/AM/CRC
wire-format bytes are explicitly out of scope. It takes an `InterruptLine`
(see above) rather than a hardware-specific interrupt controller, which is
what let the Z80 side reuse it with zero code changes.

Each drive is backed by a `VirtualDisk` (`FileVirtualDisk` is the only
implementation): a flat, track-ordered file prefixed with a 16-byte
self-describing header (`"SCPU"` magic + heads/tracks/sectors/sectorSize).

Read/Write Sector transfer one byte at a time through the Data register,
raising an interrupt after each byte except the last - real interrupt-per-
byte DRQ timing. Format Track and Seek/Restore complete synchronously
within a single register write, with one completion interrupt.

## Video (the console on both CPUs)

`VideoController` (`modules/Video`) is a CRTC-style display controller -
register layout and semantics modeled on the real Motorola 6845 (the chip
real CP/M-era machines like the Kaypro and Osborne actually used), the sole
console on both CPUs (it fully replaced an earlier `ConsoleDevice`, which no
longer exists). Genuinely memory-mapped: the CPU pokes ASCII bytes directly
into a separate character-RAM device at ordinary addresses, and the
controller itself only holds the registers that say how to interpret that
RAM.

Five bytes of register space:

```
+0  Address Register (write-only) - selects which of R0-R17 the next
    access to +1 refers to
+1  Data Register (read/write) - gets/sets the selected register's value
+2  Keyboard Data (read) - dequeues the next keystroke, or 0 if none queued
+3  Keyboard Status (read) - bit 0 set if a keystroke is queued
+4  Mode Control (read/write) - MODE_TEXT (default) or MODE_GRAPHICS; not
    one of the real 18 6845 registers - real CGA-class hardware kept this
    switch in a separate latch outside the CRTC too
```

All 18 real 6845 registers are readable/writable, but only these drive the
simplified rendering: **R1** Horizontal Displayed, **R6** Vertical
Displayed, **R10** Cursor Start (used only as an on/off flag), **R12/R13**
Display Start Address, **R14/R15** Cursor Address (both as RAM offsets, not
row/col). Everything else is stored and reads back correctly but isn't
acted on.

**Hardware scrolling**: `DO_PUTCH` (both `BIOS.asm` files) moves Display
Start Address by one row on scroll instead of copying every row up or
wrapping back to row 0 - the CRTC's own `R12`/`R13` do the work, matching
real period hardware. Character RAM is a 2048-byte ring, bigger than the
1920 bytes actually visible, so there's always at least one spare row to
scroll into.

**Graphics mode**: a fixed 320x200 1-bit-per-pixel bitmap, addressed
row-major (`byteOffset = row*40 + col/8`, MSB-first) - not
register-configurable, same as real CGA's fixed-resolution modes.
`BIOS_SET_PIXEL`/`bios_set_pixel()` is the only way into it; `bios_putch()`/
text rendering keep working on the character RAM underneath regardless,
same as real CGA hardware kept its text framebuffer intact under a graphics
mode.

Arrow/Page Up/Down keys queue synthetic codes `0x80-0x85` (`AWT`'s
`keyTyped` never fires for them) - real extended-keyboard firmware
convention for "a key with no character."

No vertical-sync interrupt is modeled (`raiseInterrupt()` is a no-op) -
rendering is driven by a periodic Swing `Timer` once `show()` is called, not
by individual writes.

## CP/M filesystem (host-side reference implementation)

`CpmFileSystem` (`modules/Disk`) is a CP/M-style filesystem - the obvious
choice given the IBM 3740 geometry this project emulates *is* the classic
CP/M 8" SSSD disk format. Flat directory (no subdirectories) of 32-byte
entries ("FCBs"), 1KB allocation blocks, track 0 reserved for boot/system
use, tracks 1-75 as the data area. A directory entry ("extent") holds 16
block pointers (16KB); a file bigger than that gets multiple entries sharing
its name, distinguished by the `EX` (extent) field - real CP/M's own
mechanism. Supports create/read/write/delete/rename, all extent-aware.

This operates directly on a `FloppyDevice` - no `FloppyController`
needed, since it's pure filesystem logic over raw sector read/write.
Crucially, **it never runs inside either emulated CPU** - it exists to
validate the on-disk byte format against something other than hand-written
firmware code, and as `CpmDiskTool`'s host-side CLI (`format`/`put`/`get`/
`list`/`erase`/`bootimage`) for preparing disk images. BDOS (`bdos.c`)
independently implements the *same* on-disk layout, the way one prototypes
an algorithm on a host before porting it to the real embedded target.

### Reading real period CP/M disk images

`modules/Disk/.../Imd/` reads real `.IMD` (ImageDisk, dunfield.com)
preservation-format disk images, deliberately kept separate from
`CpmFileSystem`/BDOS:

- **`ImdImage`** decodes the ImageDisk container format itself - comment
  header, per-track mode/cylinder/head/sector-size, the sector numbering
  map, all 8 real sector-data record variants (including compressed
  fill-byte runs) - CP/M-agnostic, just the physical container.
- **`DiskParameterBlock`** + **`CpmImageReader`** then read an actual CP/M
  directory/files against *configurable* reserved-records/records-per-
  block/directory-blocks/block-pointer-width, rather than this project's
  own fixed constants - a genuinely separate reader from `CpmFileSystem`,
  since the only consumer of that configurability is reading real external
  images, not this project's own disks.
- **`ImdTool`** is the CLI (`list`/`get`), validated against a real TRS-80
  Model II CP/M 2.2 8" disk image - its ~60-file directory lists correctly
  and a real text file reads back byte-for-byte, confirming this project's
  own CP/M format understanding against independently-created data.

## c-shared: one C codebase, two toolchains

`modules/cpu/c-shared` (not a Maven module, just files) holds every piece of
C shared unchanged between both CPUs: `bios.h`/`bdos.h` (declarations),
`bios_util.c` (`bios_putstr`/`bios_clear_eol`/`bios_clrscr` - ordinary C
built from existing primitives, no new hardware operation), `bdos.c` (BDOS's
real implementation, see above), `ccp.c` (the shell), `edit.c` (a full-screen
editor), `graphics.c`/`hello.c` (demos). Compiled by `cc65`/`cl65` for the
6502 build, `SDCC` for the Z80 build.

**Bridging C to hand-assembled firmware.** `bios_glue.s`/`bdos_glue.s`
(6502, ca65 syntax) and `bios_glue.asm`/`bdos_glue.asm` (Z80, sdasz80
syntax) are each CPU's own thin layer turning a jump-table address into an
ordinary C function call. Multi-parameter BIOS primitives (read/write
sector, etc.) use a "stage one value, then trigger" sequence rather than one
function taking every parameter at once - built for cc65's `__fastcall__`
limit (one parameter, received directly in register, no software stack
involved), not any real Z80 limitation (`BIOS.asm`'s own hand-written Z80
callers pass a second/third value directly in `E`/`HL`). SDCC's own default
`sdcccall(1)` convention already passes a single byte/word argument in a
register too, so `bios.h`'s `BIOS_FASTCALL` macro is `__fastcall__` under
cc65, empty under SDCC - confirmed by compiling probes and reading the
emitted assembly directly, not just trusting docs.

**A deliberately minimal "project's own time_t."** `bios_get_time()`
(`unsigned long *out`) takes its pointer directly - a true single parameter,
so no staging split needed at all, same style as `bios_set_buffer()`. There
is no `<time.h>` dependency on either toolchain; calendar/display conversion
lives entirely in `ccp.c` (see CCP below), not in this header.

### CCP.COM (`ccp.c`) - the shell

`DIR`, `STAT`, `DATE`, `TYPE <name>`, `ERA <name>`, `COPY <src> <dst>`,
`REN newname=oldname`, `SAVE <pages> <filename>`, bare `X:` drive
switching, and "type a name to run it" (defaulting a missing extension to
`.COM`) - all calling into BDOS for filesystem logic and BIOS for hardware
access, never touching either directly. `STAT` prints "XXXk used, YYYk
free, ZZZk total" via a hand-rolled `print_decimal()` (no `sprintf` - no
libc headers are included anywhere in this shared C layer on either
toolchain). `DATE` prints `yyyy-MM-dd HH:mm:ss` from `bios_get_time()` - real
CP/M 2.2 has no built-in DATE command of its own, this is purely this
project's own addition, same spirit as `STAT`. Its epoch-to-calendar
conversion (`epoch_to_civil()`) is a plain year-then-month subtraction loop,
not a closed-form days-since-epoch formula chaining divisions by awkward
constants - this runs once per typed command, not once per character, so
easy-to-trace-by-hand code won over a denser one.

### EDIT.COM (`edit.c`) - a full-screen text editor

A minimal nano-style editor, ported from a third-party C64 program
(AdveditC64, MIT licensed). Required `BIOS_GOTOXY` (the first way to reach
an arbitrary screen cell from C) and arrow-key support in `VideoController`.
File I/O goes through the same BDOS record calls `CCP.COM`'s `TYPE`/`COPY`/
`SAVE` already use. Open/Save/Exit are Ctrl+O/S/X. Pages larger than one
screen are handled via a page-swap mechanism (`EDITPxx.$$$` temp files) -
the thing that originally surfaced the shared BDOS `fcb_name`-clobbering bug
described in project memory.

## Z80 core

`com.mj.z80.Z80CPU` holds the full register file (including the shadow set,
`IX`/`IY`, `I`/`R`) plus references to both buses (`memoryBus`/`portBus`).
Deliberately kept a plain, synchronous object - no threading, no run loop of
its own - since every instruction test uses it directly; `Z80Main` provides
the real threaded run loop (`serviceInterrupts()` + `step()` in a tight
loop, on its own daemon thread, same reasoning the 6502 side's own `CPU`
class extends `Thread` for).

The instruction set is essentially complete. Deliberately out of scope, not
forgotten: IM0 (every real use is just `RST n` anyway, which IM1 already
covers), block I/O (`INI`/`OTIR`/etc. - the owner's own call: the port
address and the loop counter share a register in a way that's awkward to
model well, and not a priority gap given real first-hand experience writing
an actual interrupt-driven print spooler instead of using them), the
undocumented opcodes (`IXH`/`IXL` direct access, doubled `DD`/`FD` prefixes,
`SLL`), and cycle-accurate timing.

### Interrupts and IM2 vector wiring

`IFF1`/`IFF2`, `EI`/`DI`, `IM 0/1/2`, `RETN`/`RETI` all work the standard
way. NMI always wins over a maskable interrupt, is serviced regardless of
`IFF1`, and jumps to the fixed `$0066` (not `$0038`, IM1's own fixed
vector). IM1 pushes PC and jumps to `$0038`. **IM2** pushes PC and reads a
real 16-bit pointer from the vector table at `(I<<8)|vector` - the vector
byte itself comes from a real device, not a placeholder:

- **`InterruptVectorSource`** (`modules/Devices`: `hasPendingInterrupt()`/
  `vector()`/`acknowledge()`) plus `PBus.registerInterruptVectorSource()`/
  `acknowledgePendingVector()` - priority-ordered (first pending source
  wins), and presenting the vector *is* the acknowledgment, same as real
  hardware's interrupt-acknowledge cycle.
- **`VectoredInterruptController`** (`implements Device,
  InterruptVectorSource`) is the first real source - one register (the
  vector byte, loaded via a plain `OUT`), modeling a small PIO/CTC-like
  peripheral rather than living on `FloppyController` directly: real
  WD179x-style FDCs never had a Z80 vector register themselves, a real
  system routed the FDC's `INTRQ` line into a separate PIO/CTC chip that
  did. It registers itself as both a vector source *and* an ordinary
  port-bus device - two deliberately independent registrations.
- `Z80CPU.serviceInterrupts()`'s IM2 branch asks
  `portBus.acknowledgePendingVector()` first, falling back to a manual
  `pendingInterruptVector` field only when nothing's registered (keeps
  every raw-CPU-level interrupt test working without a whole device graph).

Making `BIOS.asm` actually use IM2 for a real operation (an interrupt-driven
disk ISR, rather than the polling it does today) was deliberately scoped out
when this was built - console and disk I/O are both polling-only in
practice, despite the CPU-level interrupt machinery existing end to end.

### Z80VideoDiskMachine

The real bootable Z80 machine, mirroring `CPUMain`'s role on the 6502 side.
Memory bus:

```
$0000-$B7FF   RAM
$B800-$BFFF   VideoController's character RAM (same address as the 6502 side)
$C000-$DFFF   VideoController's graphics RAM (same address as the 6502 side)
$E000-$FFFF   RAM
```

Port bus:

```
$00-$04   VideoController's register set
$05       VectoredInterruptController (the IM2 vector register)
$06-$09   RTC (big-endian Unix-epoch-seconds, read-only, 4 bytes)
$10-$14   FloppyController, wholesale
```

`FloppyController`'s interrupt line is wired through
`VectoredInterruptController` rather than straight onto the port bus's
shared line, matching the real-hardware INTRQ-into-PIO/CTC routing
described above - `FloppyController` itself needed zero changes either way.
No ROM/firmware-protection concept exists yet on this side (everything is
`basicMemory`); real write protection is a 6502-only feature so far.

### Z80 BIOS.asm

Same jump-table-at-`$F000` convention as the 6502 side, same addresses for
every shared primitive, but genuinely idiomatic Z80 where the two CPUs
differ: `BIOS_READ_SECTOR`/`BIOS_WRITE_SECTOR`/`BIOS_SET_PIXEL` take their
buffer/column directly in `HL` (a real 16-bit pointer register), no
zero-page staging trick needed at this level - only the shared C glue layer
(`bios_glue.asm`) needs its own private staging, to satisfy `bios.h`'s
cc65-oriented "stage one value, then trigger" convention. Every port access
goes through the ED-prefixed `IN A,(C)`/`OUT (C),A` forms (`BC` = port
number, loaded fresh each time), never the unprefixed forms - those put `A`
itself on the port address's upper byte, a real documented Z80 quirk this
project's own emulator faithfully reproduces and which would be actively
wrong for small port numbers like these.

### Z80Main - the real boot sequence

Loads `BIOS.z80` and `bdos.ihx` onto the memory bus, attaches up to 4
`z80-diskN.img` images from an optional disk-directory CLI argument
(defaulting to `.`), then loads and jumps into `Boot.z80` - a tiny
hand-assembled stub that calls `BDOS_RUN_FILE` to find `CCP.COM` on drive 0
and load-and-jump into it, landing in CCP's own real `A>` prompt. The same
real cold-boot mechanism `Test_Z80BdosRunFile` proves end to end, not a
canned demo.

### SDCC and the Z80 C build

SDCC's own crt0 relies on the classic Z80 convention that power-on
execution starts at address 0 (a real reset vector, `JP $0100` into SP
init/BSS-zeroing/`CALL _main`) - **not** `--code-loc`'s address, which only
relocates where the compiled `_CODE` area begins. A hand-written program
with no crt0 (`Demo.asm`) really does start at its own `org`; an
SDCC-compiled one must boot at `pc=0` instead (found the hard way - the
first stray `RETI` corrupted execution when this was gotten wrong).

BDOS and any `BDOS_RUN_FILE`-loadable "app" (`ccp.ihx`, `edit.ihx`) both
build with `--no-std-crt0`, stripping the reset-vector/crt0 regions
entirely - required since BDOS and whichever app needs it get loaded into
the same 64K space at once, and two full crt0s would collide at address 0
and `$0100`. BDOS needs no crt0 of its own at all (only ever entered through
its jump table); `ccp.ihx`/`edit.ihx` use `cpm_crt0.asm` - a genuinely
minimal `call _main / ret`, with no SP reset (Z80's SP is a real hardware
register BDOS's own `CALL`/already-running code depends on staying intact)
and no BSS zeroing (that work still has to happen somewhere - SDCC emits it
into a shared `gsinit` routine regardless of `--no-std-crt0`, which
`cpm_crt0.asm` calls explicitly, copied verbatim from SDCC's own
`crt0.s`).

`--data-loc` is pinned explicitly (`$3000`) for every app build - left
unspecified, SDCC's own default places `_DATA`/`_BSS` at `$8000` with no
size check against anything else in the address space; `edit.c`'s
16000-byte `lines[][]` buffer alone pushed `_DATA`'s end into video RAM
before this was caught.

## RTC

`RTC` (`modules/Devices`) is a plain 4-byte, read-only, big-endian
Unix-epoch-seconds register block, wired into both real machines (6502
`$9025-$9028`, Z80 port `$06-$09`) - it previously only existed in test
scaffolding. Its constructor takes an `InterruptLine` and `AddressRange`
(matching `FloppyController`/`VectoredInterruptController`'s own pattern),
and it works in two modes:

- **Started** (`.start()` called on its own `Thread`): ticks once a second,
  refreshing its register and raising the given interrupt line each time -
  the original VIA-interrupt-driven design, still used by the 6502's own
  `Test_RTC`/`RTCInt.asm` fixture.
- **Never started** (both real machines - neither ever calls
  `bus.startDevices()` for any device): `read()` self-refreshes on the first
  byte of a 4-byte read instead, giving a genuinely live clock through plain
  polling with no interrupt wiring needed at all. Skipped once the thread
  *is* running, so a caller mid-copy during an ISR can't see a torn read.

`BIOS_GET_TIME` (both `BIOS.asm` files) reads RTC's hardware bytes and
writes them into the caller's buffer in reversed, little-endian order -
RTC's own hardware register is big-endian, but both CPUs (and C's own
`unsigned long`) are little-endian, so the primitive does the conversion
rather than leaving it to the caller.

## Where this is headed

Both CPUs are at full functional and application-suite parity (BIOS/BDOS/
CCP/EDIT.COM, console/disk/graphics/RTC primitives, real disk boot). Known,
deliberately-out-of-scope gaps, not forgotten:

- Z80 block I/O instructions, undocumented opcodes, cycle-accurate timing,
  and IM0 (see Z80 core above).
- Making `BIOS.asm` actually use IM2 for a real interrupt-driven operation,
  rather than the polling it does today.
- `CpmImageReader`/`DiskParameterBlock` don't yet support mixed-density
  CP/M disks (per-track-varying sector sizes) - several real `.IMD` images
  on hand use this and aren't yet readable.

No further work has been requested in any of these areas as of this
writing.
