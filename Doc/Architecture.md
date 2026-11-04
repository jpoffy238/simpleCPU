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
$0300-$7FFF   BOOTADDR onward - where B loads a boot image; a full-size (25-
              sector) image runs well past the $0300-$03FF page into the
              free RAM beyond it, which is expected, not a collision
$8000-$8FFF   Unmapped (a bus access here faults, recoverably - exercised
              directly by Test_BIOS's fault-recovery test)
$9000-$900F   VIA6522
$9010-$9014   FloppyController (status/command, track, sector, data,
              drive-select)
$EC00-$EC01   ConsoleDevice (data, status/control)
$F000-$F7FF   ROM chip 1: BIOS primitives (see Firmware below)
$F800-$FFFF   ROM chip 2: Monitor, including the hardware + fault vectors
```

Zero page, stack, and RAM up to `$7FFF` are plain `basicMemory`. The gap
between `$9015` and `$EBFF`, and between `$ED00` and `$EFFF`, is
deliberately left unmapped rather than packed tight, matching how real
systems leave room between device address windows.

## Firmware: a two-ROM BIOS/monitor split

The firmware is split into two ROM chips (two `basicROM` instances, two
`.asm`/`.hex` files), mirroring real systems that had distinct BIOS and
monitor/BASIC ROM sockets:

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
| `B` | `B` | boot: load a boot image from track 0 to `BOOTADDR` and JSR there |

`B`'s boot image format: track 0 sector 1 is a 1-byte header (how many code
sectors follow, clamped to 25); sectors 2 onward are the code itself, read
consecutively into `BOOTADDR` and beyond. Capped at 25 code sectors (3200
bytes) because track 0 is exclusively reserved for boot/system use - tracks
1-75 are the CP/M filesystem's data area, and boot loading must never spill
into it. A cc65-compiled "hello world" is already 192 bytes (see below), so
this was a near-term necessity, not a hypothetical one.

Both ROMs load via `basicROM`'s privileged constructor-time path (the file
is read and written directly into the backing array, bypassing the normal
write-protection check) rather than the plain-bus-write loader
(`CPU_CreateUtil.load()`) used for test programs that are genuinely meant
to live in RAM - these ROMs are truly write-protected at runtime.

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
as a 64-entry directory. Files are capped at one "extent" (16 block
pointers per directory entry = 16KB) - real CP/M's multi-extent mechanism
for bigger files isn't implemented. Record counts, and so file lengths
read back, are only tracked to the nearest 128-byte record, same
limitation as real CP/M.

This operates directly on a `FloppyDevice` - no `FloppyController`/VIA
needed, since it's pure filesystem logic over raw sector read/write, not
hardware register access. Crucially, **it never runs inside the emulated
6502** - there's no bridge between the JVM and the CPU's instruction
stream. It exists to validate the on-disk byte format (especially the one
genuinely fiddly bit: block<->(track,sector) translation, since 26
sectors/track isn't divisible by 8 sectors/block, so blocks routinely span
a track boundary) against something other than hand-written 6502 code, and
as `CpmDiskTool`'s host-side CLI (`format`/`put`/`get`/`list`) for
preparing disk images before any 6502-resident DOS exists to do it from
inside the emulator. The real DOS - `DIR`/read/write/delete at the monitor
prompt - is still unwritten 6502-side code (now likely C, see below), which
would implement the *same* format independently, the way one prototypes an
algorithm on a host before porting it to the real embedded target.

## cc65 (C on the 6502 side)

More advanced 6502-side work - the DOS layer above, chiefly - is planned to
be written in C via `cc65` rather than hand assembly, specifically because
the block<->(track,sector) division above is exactly the kind of thing
that's natural in C and genuinely awkward in 6502 asm (no divide
instruction). `modules/cpu/6502/src/main/c/` has a validated starting
point:

- **`bios_glue.s`/`bios.h`** bridge C to `BIOS.asm`'s jump table. Multi-
  parameter primitives (read/write sector, format track) are split into a
  "stage one value, then trigger" sequence rather than one function taking
  every parameter at once, since correctly popping more than one cc65
  software-stack parameter in hand-written glue isn't worth the risk;
  `__fastcall__` for a single parameter (received directly in A, no
  software stack involved) is simple and well-documented.
- **`simplecpu.cfg`** is a custom cc65 linker config (not a stock cc65
  target - this is fully custom hardware). Its `ZEROPAGE` segment starts
  at `$09`, right after `MONITOR.asm`'s own `$00-$08` - for debugging
  clarity, not functional necessity: DOS and the monitor never run
  concurrently (it's a sequential handoff via `B`, not interleaved
  execution), so there's no actual zero page to divide between them.
- **`hello.c`** is a build smoke test, validated end-to-end against the
  real toolchain (not just planned) - it compiles and links into a
  192-byte binary. That number is also what originally confirmed the
  multi-sector boot problem above was real and immediate, not
  hypothetical.

Not yet done: actually running cc65-compiled code inside the emulator.

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

- **A real DOS.** The eventual goal is for `B` to load a real disk
  operating system - file create/edit, maybe on-machine compilation -
  sitting entirely on top of the primitives ROM's raw sector read/write,
  the same way the monitor does today. The primitives ROM deliberately
  knows nothing about filesystems, directories, or files; that's additive
  on top, not a reason to change what's there now. The on-disk filesystem
  format is validated (see CP/M filesystem above) and the C toolchain is
  validated (see cc65 above) - what's left is writing the actual 6502-side
  commands (`DIR`/read/write/delete at the monitor prompt) and running them
  for real inside the emulator, which hasn't been attempted yet.
- **`modules/Video`** is reserved but unstarted.
