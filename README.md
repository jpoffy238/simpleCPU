# simpleCPU

A Java emulator for the NMOS 6502, built up from a validated CPU core through
a device/bus abstraction, a two-ROM BIOS/monitor firmware split, and a
WD179x-style floppy disk subsystem.

See [Doc/Architecture.md](Doc/Architecture.md) for how the pieces fit together.

## Modules

- `modules/Devices` — the CPU-agnostic bus (`PBus`/`DeviceBus`/`AddressRange`/
  `Device`), the MOS 6522 VIA interrupt controller, the console device, and
  the RTC.
- `modules/cpu/6502` — the 6502 core, the interactive `CPUMain` entry point,
  and the BIOS/monitor firmware (`src/main/asm`).
- `modules/Disk` — the WD179x-style `FloppyController` and file-backed
  virtual disks.
- `modules/Video` — reserved for a future graphics device; not yet
  implemented or registered in the build.

## Building

```
mvn install
```

Building `modules/cpu/6502` also assembles the 6502 firmware (`BIOS.asm`,
`MONITOR.asm`, and various test programs) via `crasm`, which must be on your
`PATH`.

## Running

```
java -jar modules/cpu/6502/target/emu6502-0.0.7-SNAPSHOT-jar-with-dependencies.jar
```

This opens a Swing console window running the monitor ROM. Type `H` at the
`>` prompt for a list of commands.

## Testing

```
mvn test
```

Add `-DslowTests=true` to also run Dormann's 6502 functional test and
Clark's ADC/SBC decimal-mode program.
