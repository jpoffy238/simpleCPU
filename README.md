# simpleCPU

Two Java-emulated CP/M-era CPUs - a validated NMOS 6502 core and a Z80
core - sharing a device/bus abstraction, a WD179x-style floppy disk
subsystem, a CRTC-style video console, and (since both run the same
CP/M-flavored DOS) a shared C codebase for BDOS/CCP/BIOS-primitive glue.

See [Doc/Architecture.md](Doc/Architecture.md) for how the pieces fit
together.

## Modules

- `modules/Devices` — the CPU-agnostic bus (`PBus`/`DeviceBus`/
  `AddressRange`/`Device`), `InterruptLine`, the MOS 6522 VIA interrupt
  controller (6502-only hardware), the RTC, and `InterruptVectorSource`
  (Z80 IM2 interrupt-vector support).
- `modules/cpu/6502` — the 6502 core, the interactive `CPUMain` entry
  point, and the 6502-specific firmware (`src/main/asm`, assembled via
  `crasm`).
- `modules/cpu/z80` — the Z80 core, the interactive `Z80Main` entry point,
  and the Z80-specific firmware (`src/main/asm`, assembled via the
  `z80-asm` toolchain).
- `modules/cpu/c-shared` — plain C source (not a Maven module) shared
  unchanged between both CPUs: BDOS, the CCP shell, the EDIT.COM editor,
  and BIOS-primitive glue headers. Compiled by `cc65` for the 6502 side,
  `SDCC` for the Z80 side.
- `modules/Disk` — the WD179x-style `FloppyController`, file-backed
  virtual disks, a host-side CP/M filesystem reference (`CpmFileSystem`),
  and a reader for real period `.IMD` CP/M disk images (`Imd/`).
- `modules/Video` — `VideoController`, a CRTC-style display/keyboard
  controller (text + 320x200 1bpp graphics) - the sole console on both
  CPUs.

## Building

```
mvn install
```

Building `modules/cpu/6502` also assembles its firmware (`BIOS.asm`,
`MONITOR.asm`, and various test programs) via `crasm`, which must be on
your `PATH`. Building `modules/cpu/z80` likewise assembles `BIOS.z80` and
its own test fixtures via `z80-asm`, which must also be on your `PATH`.

BDOS and the CCP/EDIT "apps" shared via `modules/cpu/c-shared` are *not*
part of the Maven build - they're a manual step (`make` in each CPU's own
`src/main/c`, needing `cl65`/`cc65` for the 6502 side or `sdcc`/`sdasz80`/
`sdldz80` for the Z80 side on `PATH`). Tests that depend on them (e.g.
`Test_CCP`, `Test_Edit`, `Test_Z80CCP`) skip themselves if the binaries
haven't been built yet.

## Running

```
java -jar modules/cpu/6502/target/emu6502-0.0.7-SNAPSHOT-jar-with-dependencies.jar [disk-dir]
java -jar modules/cpu/z80/target/emuZ80-0.0.1-SNAPSHOT-jar-with-dependencies.jar [disk-dir]
```

Both open a Swing console window. The 6502 jar boots into its MONITOR
ROM's `>` prompt (type `H` for a list of commands; `B` boots `CCP.COM` off
disk). The Z80 jar boots straight into a real CP/M cold boot, landing in
`CCP.COM`'s own `A>` prompt directly. The optional `disk-dir` argument
names a folder holding `diskN.img` (6502) / `z80-diskN.img` (Z80) disk
images, defaulting to the current directory.

## Testing

```
mvn test
```

Add `-DslowTests=true` to also run Dormann's 6502 functional test and
Clark's ADC/SBC decimal-mode program.
