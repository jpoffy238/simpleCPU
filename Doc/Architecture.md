# simpleCPU Architecture

A Java emulator for the NMOS 6502, built bottom-up: a validated CPU core,
a CPU-agnostic device/bus layer, a two-ROM BIOS/monitor split, and a
WD179x-style floppy disk subsystem. This document describes how those
pieces fit together as of the `modules/cpu/6502` + `modules/Devices` +
`modules/Disk` split (post BIOS/monitor ROM separation).

## Module layout

```
modules/
  Devices/        CPU-agnostic bus: Device/PBus/DeviceBus/AddressRange,
                   VIA6522, ConsoleDevice, RTC, device exceptions.
  cpu/6502/        The 6502 core (com.mj.cpu001.CPU), the interactive
                   CPUMain entry point, and the firmware (src/main/asm).
  Disk/            FloppyController (WD179x-style) and file-backed
                   virtual disks (VirtualDisk/FileVirtualDisk/IBM3740).
  Video/           Reserved for a future graphics device - not yet
                   implemented, not registered in the root build.
```

`Devices` exists as its own module so that a future non-6502 CPU core could
reuse the same bus/VIA/console without duplicating them - `cpu/6502` and
`Disk` both depend on it, but it depends on neither.

## The bus

Every device (`Device` interface) sits behind a shared `PBus`
(`DeviceBus` is the concrete implementation). A device registers an
`AddressRange`; `DeviceBus.read()/write()` linearly scans registered ranges
to find the device that `contains()` a given address and delegates to it.
There's no bank switching or address decoding beyond this - a read/write to
an address with no device behind it throws `DeviceUnavailable`, which the
CPU treats as a recoverable bus fault (see below), not a crash.

## CPU core

`com.mj.cpu001.CPU` is the 6502 itself: registers, flags, the fetch/decode/
execute loop (`run()`), and the stack (hardwired to `$0100-$01FF`, as on
real hardware). Most instructions are table-driven (`Firmware.Table`);
execution-flow instructions (`JMP`/`JSR`/`RTS`/`RTI`/`BRK`/branches) and the
project-specific `MIH` ("maskable interrupt handler" injection) and `HLT`
instructions are hand-written in `Firmware.ExecutionFlow`. `HLT` is a
project-specific instruction (not real 6502) used by unit tests to signal
"this program is done" - it halts the whole emulated CPU and is never meant
to be used by real firmware.

### Interrupts and the bus-fault vector

`fetchInstruction()` checks for a pending interrupt before every opcode
fetch, injecting a synthetic `MIH` instruction that reads the right vector
and transfers control, same as real 6502 hardware timing. Three vector
pairs exist: NMI (`$FFFA`), RESET (`$FFFC`), IRQ (`$FFFE`) - standard 6502 -
plus one project-specific extension:

- **`VECTOR_FAULT` ($FFF8)**: reading or writing an address with no device
  behind it throws `DeviceUnavailable`. Rather than halting the emulated
  machine outright, `CPU.run()` pushes PC+status (like a real interrupt)
  and jumps through this vector, giving firmware a chance to recover. The
  monitor ROM's fault handler discards the pushed state and returns
  straight to the prompt rather than attempting `RTI`, since resuming the
  faulting instruction would just fault again.

### Interrupt aggregation (VIA6522)

Every device that wants to interrupt the CPU goes through a `VIA6522`
(real MOS 6522 register semantics for IFR/IER) rather than touching the
bus's shared IRQ line directly - the VIA is the single point that
aggregates per-device interrupt sources onto that one line, same as real
hardware. Only the interrupt subsystem (IFR/IER) is functional; ports,
timers, and the shift register are addressed but inert. Current sources:
`RTC` via `FLAG_T1`, `FloppyController` via `FLAG_CB1`, `ConsoleDevice` via
`FLAG_CA1` (gated by its own control-register interrupt-mode bit).

## Memory map

The address space as `CPUMain`/`Test_BIOS` wire it up:

```
$0000-$00FF   Zero page (monitor-private vars + the BIOS-call ABI's
              ADDRLO/ADDRHI parameter-passing slot)
$0100-$01FF   Stack (fixed by 6502 hardware)
$0200-$02FF   INBUF (the monitor's command-line buffer)
$0300-$7FFF   BOOTADDR onward - where B loads CCP.COM's content; any file
              beyond a page or so runs well past the $0300-$03FF page
              into the free RAM beyond it, which is expected, not a
              collision
$8000-$81FF   BDOS's own working RAM (directory-scan position, "currently
              open"/"currently being written" file state, block-allocation
              bitmap, sector buffer) - plain `basicMemory`, not part of the
              ROM chip below, since BDOS's C code writes to it at runtime
              (see BDOS below)
$8200-$8FFF   Unmapped (a bus access here faults, recoverably)
$9000-$900F   VIA6522
$9010-$9014   FloppyController (status/command, track, sector, data,
              drive-select)
$9015-$9FFF   Unmapped ($9100 is `Test_BIOS`'s fault-recovery probe address)
$A000-$B7FF   ROM chip 3: BDOS (see Firmware below) - sized generously
              (6KB) relative to what BDOS actually uses, since this whole
              range was otherwise free address space anyway
$EC00-$EC01   ConsoleDevice (data, status/control)
$F000-$F7FF   ROM chip 1: BIOS primitives (see Firmware below)
$F800-$FFFF   ROM chip 2: Monitor, including the hardware + fault vectors
```

Zero page, stack, and RAM up to `$7FFF` are plain `basicMemory`. The gap
between `$B800` and `$EBFF`, and between `$ED00` and `$EFFF`, is
deliberately left unmapped rather than packed tight, matching how real
systems leave room between device address windows.

## Firmware: a three-ROM BIOS/monitor/BDOS split

The firmware is split into three ROM chips (three `basicROM` instances),
mirroring real systems that had distinct BIOS, monitor/BASIC, and (in
spirit) BDOS ROM sockets - though unlike the first two, BDOS is cc65-
compiled C, not hand assembly:

### BIOS.asm - primitives ROM ($F000-$F7FF)

Raw hardware access only - console I/O and disk I/O - no command parsing
or filesystem concepts. Exposes a fixed, frozen jump table at `$F000`:

| Address | Routine             | Calling convention |
|---------|----------------------|---------------------|
| `$F000` | `BIOS_PUTCH`         | A = char to print |
| `$F003` | `BIOS_GETCH`         | blocks; returns A = char |
| `$F006` | `BIOS_SELECT`        | A = drive 0-3 |
| `$F009` | `BIOS_READ_SECTOR`   | A = track, X = sector, ADDRLO/ADDRHI = dest |
| `$F00C` | `BIOS_WRITE_SECTOR`  | A = track, X = sector, ADDRLO/ADDRHI = src |
| `$F00F` | `BIOS_FORMAT_TRACK`  | A = track, X = fill byte (one track only) |
| `$F012` | `BIOS_RESTORE`       | no parameters - homes the selected drive to track 0 |

Each slot is a `JMP` to the real implementation below it - moving an
implementation is always safe as long as its slot still jumps to the right
place. `MONITOR.asm` is assembled completely separately (no shared symbol
table between the two files), so it hardcodes these same six addresses as
constants - a frozen ABI, documented in both files, that must be kept in
sync by hand if anything here ever moves.

### MONITOR.asm - monitor ROM ($F800-$FFFF)

The `COMMAND ARGUMENTS`-style command interpreter, and the thing the reset
vector actually lands in. Never touches `CONSOLE_DATA`/`FD_STATUS`/etc.
directly - only the jump table above. Also owns the hardware vector table
(`$FFFA-$FFFF`) and the fault vector (`$FFF8`), embedded directly via `DW`
directives, since reset should land in the monitor and fault recovery
("print `?`, return to the prompt") is monitor policy, not a hardware
primitive.

NMI and RESET both vector through `RESET_INIT`, not straight to `LOOP`:
it selects drive 0 and issues `BIOS_RESTORE` before falling into the normal
prompt loop, so a reset always finds the hardware in a known state
regardless of what a previous program (a future DOS, say) left the
drive/track selection as. This is a belt-and-suspenders robustness measure,
not a correctness requirement - the monitor's own ROM code never needs
"restoring" after something else runs, since ROM is write-protected and the
reset vector is unconditional (a reset always lands back here no matter
what was executing), so the monitor was never actually torn down, just not
the thing currently running. IRQ isn't meaningfully used by this system
yet, and conceptually shouldn't re-init hardware mid-operation even if it
were, so it goes straight to `LOOP`.

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

`B` no longer reads track 0 at all. It asks BDOS to find a file named
`CCP.COM` (`BDOS_FIND_FILE`), then repeatedly asks for its next 128-byte
record (`BDOS_READ_RECORD`), appending each one at `BOOTADDR` onward,
exactly the way real CP/M's cold loader found and loaded its own CCP - a
file in the directory, not a fixed, hand-maintained boot area. If `CCP.COM`
isn't found, `B` prints `?` and returns to the prompt, same as an unknown
command. This replaced an earlier convention where track 0 sector 1 was a
1-byte header (how many 128-byte code sectors followed, capped at 25 so
loading could never spill into track 1+) and sectors 2 onward were the raw
code - abandoned because the cap (3200 bytes) was already tight against a
cc65-compiled shell (CCP.COM, with `DIR`/`TYPE`/`ERA`/`COPY`, is in the low
thousands of bytes), where a CP/M file's size - now that BDOS supports
multi-extent files (see CP/M filesystem below) - isn't capped at all short
of running out of disk. Track 0 is still reserved/unused by the filesystem
- just not read by `B` anymore.

All three ROMs load via `basicROM`'s privileged constructor-time path (the
file is read and written directly into the backing array, bypassing the
normal write-protection check) rather than the plain-bus-write loader
(`CPU_CreateUtil.load()`) used for test programs that are genuinely meant
to live in RAM - these ROMs are truly write-protected at runtime. BDOS's is
a raw binary (`loadBinary()`), not Intel HEX (`load()`) - see BDOS below.

**Self-modifying code and real write protection.** `E`'s implementation has
no way to `JSR` to a runtime-computed address (6502 `JSR` only takes a
literal operand), so it writes a fresh `JSR <addr>` instruction into a
small scratch area before calling it - the classic monitor-ROM
self-modifying-code technique. That scratch area has to live in zero page
(writable RAM), not inline in the ROM's own code: an earlier version kept
it inline, which worked back when the whole BIOS loaded into RAM via plain
bus writes, but broke the instant the monitor became a real ROM (`STA`
into ROM throws `ROException`, which `CPU.run()` just logs and continues -
the patch silently never happened).

**Branch-range safety.** `DISPATCH`'s command-letter comparisons branch to
small, nearby `JMP` stubs rather than branching to the real handlers
directly. A direct `BEQ` must reach its target within a single signed byte
(-128..127); as commands were added, the handler blocks for letters
checked early in the chain kept landing outside that range (hit twice:
once in `CMD_DUMP`'s dump loop, once in `DISPATCH` itself). Indirecting
through one-instruction `JMP` stubs - which have no range limit - closes
off that whole bug class instead of re-patching it per command.

### BDOS - filesystem ROM ($A000-$B7FF)

The CP/M filesystem logic (directory scan, find file by name, read a
file's content record by record) - see CP/M filesystem below for the
on-disk format itself. Unlike BIOS.asm/MONITOR.asm, this is cc65-compiled C
(`bdos.c`), not hand assembly - exactly the C toolchain described under
cc65 below, used for its first real piece of ROM-resident firmware rather
than just a RAM-loaded program. Exposes its own fixed, frozen jump table at
`$A000` (`bdos_jumptable.s`), same contract as BIOS's:

| Address | Routine | Calling convention |
|---------|---------|---------------------|
| `$A000` | `BDOS_REWIND_DIR` | no parameters - resets the directory scan position |
| `$A003` | `BDOS_NEXT_ENTRY` | ADDRLO/ADDRHI = 32-byte dest; returns A = 1 found / 0 done |
| `$A006` | `BDOS_FIND_FILE` | ADDRLO/ADDRHI = filename (null-terminated); returns A = 1 found / 0 not found |
| `$A009` | `BDOS_READ_RECORD` | ADDRLO/ADDRHI = 128-byte dest; returns A = 1 read / 0 done |
| `$A00C` | `BDOS_DELETE_FILE` | ADDRLO/ADDRHI = filename (null-terminated); returns A = 1 deleted / 0 not found |
| `$A00F` | `BDOS_CREATE_FILE` | ADDRLO/ADDRHI = filename (null-terminated); returns A = 1 created / 0 (exists, or directory full) |
| `$A012` | `BDOS_WRITE_RECORD` | ADDRLO/ADDRHI = 128-byte source; returns A = 1 written / 0 (directory or disk full) |

Every primitive reuses `ADDRLO`/`ADDRHI` (the same pair `BIOS_READ_SECTOR`/
`BIOS_WRITE_SECTOR` already use) rather than adding a second pointer ABI -
input for `BDOS_FIND_FILE`/`BDOS_DELETE_FILE`/`BDOS_CREATE_FILE`, output
for the rest. `BDOS_FIND_FILE`/`BDOS_READ_RECORD` are stateful across calls
(BDOS remembers which file is "open for reading" and how far into it
`BDOS_READ_RECORD` has read); `BDOS_CREATE_FILE`/`BDOS_WRITE_RECORD` are
*separately* stateful the same way (which file is "open for writing," its
accumulated block list and record count) - kept as two independent state
blocks rather than one shared one, so a program could in principle have a
read and a write open at once, even though `CCP.COM`'s only such caller
today (`COPY`) happens to fully read before each write. Both pairs,
plus `BDOS_REWIND_DIR`/`BDOS_NEXT_ENTRY`'s directory scan position, are
plain C statics in `bdos.c` rather than locals, for the same reason.

`BDOS_CREATE_FILE` builds a used-block bitmap (32 bytes, one bit per
block, covering all 243 blocks this geometry has) from a single directory
scan - the same scan that also checks for a name collision and finds a
free directory entry - then creates a zero-length file immediately
visible to `BDOS_NEXT_ENTRY`/`BDOS_FIND_FILE`, same as real CP/M's F_MAKE.
`BDOS_WRITE_RECORD` allocates a new block from that bitmap every 8 records
(1KB) and rewrites the directory entry after *every* record rather than
needing a separate "close" primitive - simpler, and crash-resilient (a
mid-write abort leaves a file a few records short, never a corrupt entry
pointing at garbage blocks). `BDOS_DELETE_FILE` just flips a status byte -
a block only counts as "used" by virtue of being referenced from a
non-empty directory entry, so clearing the entry frees its blocks
implicitly, with no separate bookkeeping to update.

**Multi-extent files** (see CP/M filesystem below for what an "extent" is)
are transparent to a caller of any of these primitives - `BDOS_READ_RECORD`
and `BDOS_WRITE_RECORD` cross from one extent into the next on their own,
without the caller ever knowing a boundary was crossed. Each direction
keeps only *one* extent's state in memory at a time (16 block pointers),
not every extent up front, so a huge file costs no more RAM than a small
one: `BDOS_READ_RECORD`, on exhausting the current extent, checks whether
it was full (128 records - real CP/M's own signal that more extents
follow) and if so re-scans the directory for `EX + 1` before continuing;
`BDOS_WRITE_RECORD` does the mirror image, starting a fresh extent (a new
directory entry, same name, `EX` one higher) once the current one's 16
blocks are full. `BDOS_NEXT_ENTRY`/`BDOS_DELETE_FILE` both filter to/walk
only `EX == 0` and beyond respectively, so a multi-extent file shows up
once in a directory listing, not once per extent, and deleting one frees
every extent's blocks, not just the first's.

Called two different ways, both external to `bdos.c`'s own compiled
binary - BDOS.bin is never linked together with a caller, since they're
separate `cl65` builds:

- `MONITOR.asm` (hand assembly) JSRs straight to the fixed addresses above -
  this is how `B` finds and loads `CCP.COM` (see Commands above).
- Any cc65-compiled program calls through `bdos_glue.s`'s thin JSR stubs,
  declared in `bdos.h` - from that program's own C code (`ccp.c`'s `DIR`/
  `TYPE`, say), it looks like an ordinary function call.

**Two memory regions, not one.** `bdos.cfg` (the linker config) splits
BDOS's code (the jump table plus `bdos.c`'s compiled functions - what
`cl65` actually produces as `bdos.bin`) from its working state (the
directory-scan position, "currently open file" block list, sector buffer -
plain C statics that get written to at runtime). The first is real,
write-protected ROM at `$A000-$B7FF`; the second needs to be writable RAM,
so it's a separate `$8000-$81FF` region with no corresponding bytes in
`bdos.bin` at all (`bdos.cfg`'s `BSS` memory area has an empty `file=`) -
Java's `basicMemory` backing array starts zeroed anyway, which is all a
BSS region needs. `CPUMain`/`Test_BIOS` register a plain `basicMemory` for
that RAM and a `basicROM` (loaded via the new `loadBinary()`, not the
Intel-hex `load()` BIOS.hex/MONITOR.hex use - cc65/`ld65` has no Intel HEX
output mode) for the ROM.

**No crt0.** Nothing ever JSRs to BDOS's reset-style entry point - only to
the jump table's individual slots - so cc65's usual startup sequence
(initialize the software stack pointer `sp` at zero page `$0F`/`$10`, zero
BSS, call `main()`) never actually runs here. Zeroed BSS falls out of
Java's array default anyway, but `sp` sitting uninitialized at the *first*
BDOS call ever (`B`'s boot sequence, before any program's own crt0 has
run) is a real hazard: nearly everything in `bdos.c` touches `sp`
indirectly, since `index / SECTORS_PER_TRACK`-style division/modulo by a
non-power-of-2 constant compiles to a call through cc65's `udiv16`/`umod`
runtime helpers, which push/pop through it like any other multi-parameter
call. Confirmed the hard way (2026-11-05): with `sp` garbage, those pushes
landed on a ROM address, logged as a continuous flood of identical
"RO Memory Write attempt" errors at the same PC (the CPU doesn't advance;
it keeps retrying the faulting instruction). Fixed in `MONITOR.asm`'s
`DI_BOOT`: initialize `sp` to `$8000` (matching `__STACKSTART__`, so
`CCP.COM`'s own crt0 sets the identical value once it's running) once, at
the very first BDOS call - never repeated on a later call, since that
would stomp whatever `CCP.COM` has already pushed onto that same stack by
then. (`bdos_find_file()` additionally keeps its one local that used to be
a small array, `fcb_name`, as a file-scope static instead - cheap
insurance against a local array landing on the stack before it's known to
be safely initialized, independent of the `sp`-init fix above.)

**A second gotcha, same debugging session:** any BDOS primitive that reads
a disk sector internally (`BDOS_NEXT_ENTRY`/`BDOS_READ_RECORD`) leaves
`ADDRLO`/`ADDRHI` pointing at BDOS's own sector buffer when it returns,
not wherever the caller last set them. A caller looping over multiple
records (`DI_BOOT` loading a multi-record `CCP.COM`, or `ccp.c`'s
`cmd_dir`/`cmd_type`/`cmd_copy`) has to track its own running
pointer/buffer separately and restage `ADDRLO`/`ADDRHI` fresh before
*every* call - `DI_BOOT` missed this initially (reusing `BYTEVAL`/
`TRACKVAL`, unused during `B`, to track the real destination now) and
silently loaded everything past the first record to the wrong address.

## Disk subsystem

`FloppyController` (`modules/Disk`) is a WD179x-style register set:

```
+0  Status (read) / Command (write)
+1  Track
+2  Sector
+3  Data
+4  Drive Select (0-3)
```

Commands (upper nibble of the command byte): `$0x` Restore, `$1x` Seek,
`$8x` Read Sector, `$Ax` Write Sector, `$Fx` Format Track. Up to 4
`FloppyDevice`s can be attached (`attachDrive(0..3, device)`); `IBM3740`
is the only implementation so far - 1 head, 76 tracks (0-indexed), 26
sectors/track (1-indexed, matching the real IBM 3740's Rec# field),
128-byte sectors. Only the sector payload is emulated - gap/sync/AM/CRC
wire-format bytes are explicitly out of scope.

Each drive is backed by a `VirtualDisk` (`FileVirtualDisk` is the only
implementation): a flat, track-ordered file prefixed with a 16-byte
self-describing header (`"SCPU"` magic + heads/tracks/sectors/sectorSize).
`create()` writes this header and truncates-then-extends the file so
re-formatting an existing image actually blanks it, not just resizes it.
`open()` validates an existing file's header against the caller's expected
geometry (validate-only, not geometry-discovering) - a drive type like
`IBM3740` always knows its own fixed geometry and rejects a mismatched
image rather than adapting to whatever's on disk.

Read/Write Sector transfer one byte at a time through the Data register,
raising a VIA interrupt (`FLAG_CB1`) after each byte except the last -
real interrupt-per-byte DRQ timing. Format Track and Seek/Restore complete
synchronously within a single register write, with one completion
interrupt, since there's no actual multi-step BUSY period to emulate.
`MONITOR.asm`'s `L`/`P` commands don't service these interrupts at all -
they just drain/feed the Data register in a tight loop, which works
because the whole transfer is already complete by the time the triggering
`STA FD_STATUS`/`BIOS_READ_SECTOR` call returns.

## CP/M filesystem (host-side reference implementation)

`CpmFileSystem` (`modules/Disk`) is a CP/M-style filesystem - the obvious,
well-documented, genuinely simple choice given the IBM 3740 geometry this
project already emulates *is* the classic CP/M 8" SSSD disk format. It's a
flat directory (no subdirectories) of 32-byte entries ("FCBs"), 1KB
allocation blocks (8 sectors each), track 0 reserved for boot/system use
(see `B` above), tracks 1-75 as the data area with blocks 0-1 of that area
as a 64-entry directory. A directory entry ("extent") holds 16 block
pointers (16KB); a file bigger than that gets *multiple* entries sharing
its name, distinguished by the `EX` (extent) field at byte 12 - real
CP/M's own mechanism, implemented here (`createFile()`/`readFile()`/
`deleteFile()` all walk as many extents as a file needs; `listFiles()`
filters to `EX == 0` so a multi-extent file is listed once, not once per
extent). Every extent but a file's last is exactly full (128 records); a
record count below 128 is how a reader knows it just read the last one -
no separate end-of-file marker needed. No rename, no subdirectories - real
CP/M didn't have subdirectories either, and rename is a reasonable later
addition deliberately left out of this pass (`deleteFile()` exists).
Record counts, and so file lengths read back, are only tracked to the
nearest 128-byte record, same limitation as real CP/M. `deleteFile()`
doesn't touch data blocks at all - a block only counts as "used" by being
referenced from a non-empty entry (see `usedBlocks()`), so clearing an
entry's status byte frees its blocks implicitly.

This operates directly on a `FloppyDevice` - no `FloppyController`/VIA
needed, since it's pure filesystem logic over raw sector read/write, not
hardware register access. Crucially, **it never runs inside the emulated
6502** - there's no bridge between the JVM and the CPU's instruction
stream. It exists to validate the on-disk byte format (especially the one
genuinely fiddly bit: block<->(track,sector) translation, since 26
sectors/track isn't divisible by 8 sectors/block, so blocks routinely span
a track boundary) against something other than hand-written 6502 code, and
as `CpmDiskTool`'s host-side CLI (`format`/`put`/`get`/`list`/`erase`/
`bootimage`) for preparing disk images. The real 6502-resident
implementation of this same format - BDOS (`bdos.c`, see above) -
independently implements the *same* on-disk layout, the way one
prototypes an algorithm on a host before porting it to the real embedded
target. `bootimage` (write a raw binary onto track 0) predates BDOS and
`B`'s current find-by-name boot sequence - still usable for writing
something else to the now-otherwise-unused track 0, but no longer part of
the `CCP.COM` boot path.

## cc65 (C on the 6502 side)

More advanced 6502-side work - the DOS layer, chiefly - is written in C via
`cc65` rather than hand assembly, specifically because the
block<->(track,sector) division CP/M's layout needs is exactly the kind of
thing that's natural in C and genuinely awkward in 6502 asm (no divide
instruction). `modules/cpu/6502/src/main/c/`:

- **`bios_glue.s`/`bios.h`** bridge C to `BIOS.asm`'s jump table. Multi-
  parameter primitives (read/write sector, format track) are split into a
  "stage one value, then trigger" sequence rather than one function taking
  every parameter at once, since correctly popping more than one cc65
  software-stack parameter in hand-written glue isn't worth the risk;
  `__fastcall__` for a single parameter (received directly in A, no
  software stack involved) is simple and well-documented.
- **`bdos_glue.s`/`bdos.h`** likewise bridge C to BDOS's jump table (see
  BDOS above) - simpler than the BIOS glue, since every BDOS primitive
  takes its input (if any) via `ADDRLO`/`ADDRHI`, already staged by the
  caller's own `bios_set_buffer()` call, and returns its result directly
  in A, so there's no parameter-staging step to write at all.
- **`bdos.c`/`bdos_jumptable.s`/`bdos.cfg`** are BDOS's own implementation,
  ROM-side jump table, and linker config - see BDOS above for all three.
- **`simplecpu.cfg`** is the linker config for RAM-loaded programs
  (`CCP.COM`, `hello.c`) - not a stock cc65 target, since this is fully
  custom hardware. Its `ZEROPAGE` segment starts at `$09`, right after
  `MONITOR.asm`'s own `$00-$08`, and must exactly match `bdos.cfg`'s own
  `ZEROPAGE` segment - cc65's calling convention (the software stack
  pointer, `ptr1`-`ptr4`, etc.) has to behave consistently whichever
  program's compiled code is currently running, since BDOS executes
  nested inside a caller's own call (a true subroutine call, not the
  monitor/DOS sequential handoff `B` itself is), not as an independent
  program with its own exclusive zero page.
- **`ccp.c`** is CCP.COM: the shell loop, drive switching, `DIR`, `TYPE`,
  `ERA <name>`, `COPY <src> <dst>` - calling into BDOS for all the
  filesystem logic itself, same as it calls into BIOS for hardware access.
  `COPY` interleaves a `BDOS_READ_RECORD`/`BDOS_WRITE_RECORD` pair per
  record rather than reading the whole source into memory first, safe
  because BDOS's read-side and write-side state are independent (see BDOS
  above).
- **`hello.c`** is a build smoke test - compiles and links into a 192-byte
  binary. That number is also what originally confirmed a since-abandoned
  boot-image-size cap was a real, near-term problem (see `B` above).
- **`Makefile`** builds all of the above with `cl65` - a dedicated rule for
  BDOS (its own linker config, no generic app glue needed) and one generic
  rule for every other `.c` file ("app"). `make install` copies `bdos.bin`
  into both resource directories, the same way the hand-assembly
  `Makefile` already does for `BIOS.hex`/`MONITOR.hex` - the only cc65
  output Java loads directly; every other app becomes a file on a disk
  image by hand, via `CpmDiskTool put`.

## Console

`ConsoleDevice` (`modules/Devices`) is a Swing-backed 80x24 text window -
no polling thread; `KeyListener` pushes keystrokes directly into a queue.
Window visibility is opt-in (`show()`), never automatic, because
`CPU_CreateUtil`'s shared test fixture constructs a `ConsoleDevice` for
~190 unrelated CPU-instruction tests and must never pop up a window (it
also skips Swing setup entirely under `GraphicsEnvironment.isHeadless()`).
Only `CPUMain`, the real interactive entry point, calls `show()`. The
status register's "data available" bit and queue-depth nibble are computed
live from the input queue's size on every read, not stored, so they can
never go stale.

## Where this is headed

- **A real DOS.** `B` now boots a real shell (CCP.COM) that finds itself by
  name through BDOS rather than a fixed boot image, and that shell has
  `DIR`/`TYPE`/`ERA`/`COPY` - BDOS can read, write, and delete files of any
  size (multi-extent, not capped at 16KB), not just read small ones,
  sitting on top of the primitives ROM's raw sector read/write the same
  way the whole DOS layer does. Still missing: rename (`CpmFileSystem`,
  the host-side reference, has no rename either - see CP/M filesystem
  above).
- **`modules/Video`** is reserved but unstarted.
