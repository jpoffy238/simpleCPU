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
$0300-$03FF   Boot-sector landing area (BOOTADDR) - where B loads a sector
$0400-$7FFF   Free RAM - future boot-loaded programs land here eventually
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
| `B` | `B` | boot: load track 0 sector 1 to `$0300` and JSR there |

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

- **Multi-sector boot.** `B` currently loads exactly one 128-byte sector to
  `$0300` and jumps there - fine for small test routines, but any real
  DOS-like program won't fit in one sector. Making `B`/reset actually boot
  something substantial needs either a known sector-count convention or a
  proper two-stage loader.
- **A DOS/filesystem layer.** The eventual goal is for `B` to load a real
  disk operating system - file create/edit, maybe on-machine compilation -
  sitting entirely on top of the primitives ROM's raw sector read/write,
  the same way the monitor does today. The primitives ROM deliberately
  knows nothing about filesystems, directories, or files; that's additive
  on top, not a reason to change what's there now.
- **`modules/Video`** is reserved but unstarted.
