package com.mj.z80;

import java.nio.file.Paths;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.mj.z80.instructions.FullInstructionSet;

/** Boots a real Z80 CP/M machine, same shape as the 6502 side's own
 * CPUMain: BIOS.z80 (console/disk/graphics primitives, see
 * src/main/asm/BIOS.asm) and bdos.ihx (the BDOS "ROM" - built manually
 * via `make install` in src/main/c, same deliberate manual-step
 * convention the 6502 side's own bdos.bin already uses, since it needs
 * SDCC on PATH) go onto the memory bus first. Up to DRIVE_COUNT disk
 * images are then attached from the disk directory (args[0], defaulting
 * to the current working directory for backward compatibility) -
 * "z80-disk0.img".."z80-diskN.img", matching Z80VideoDiskMachine's own
 * naming convention (distinct from the 6502 side's own disk0.img-
 * disk3.img so both can share a directory without colliding). Finally
 * Boot.z80 (a tiny hand-assembled stub - see its own comment) is loaded
 * and jumped into: it calls BDOS_RUN_FILE to find "CCP.COM" on drive 0
 * and load-and-jump into it - the same real CP/M cold-boot mechanism
 * Test_Z80BdosRunFile already proves end to end - landing in CCP's own
 * real "A>" prompt, not a canned demo. (Demo.z80 - the old boot target -
 * is still built and used by Test_Z80Boot, just no longer what this
 * class boots.)
 *
 * Z80CPU itself stays a plain, synchronous object (used directly, no
 * threading concerns, by every instruction test), so the threading
 * lives here instead, the same reason the 6502 side's own CPU class
 * extends Thread, just kept out of Z80CPU itself since nothing about
 * instruction execution needs it. A dedicated thread (rather than just
 * looping on the main thread) is what keeps Swing's own event thread
 * free to deliver keystrokes and repaint the window while the CPU runs -
 * BIOS_GETCH's blocking poll would otherwise deadlock against a Swing
 * thread that never gets to run. */
public class Z80Main {
	private static final Logger logger = LogManager.getLogger(Z80Main.class);
	private static final int DRIVE_COUNT = 4;

	public static void main(String[] args) throws Exception {
		String diskDir = args.length > 0 ? args[0] : ".";

		Z80VideoDiskMachine machine = new Z80VideoDiskMachine();
		machine.loadIntoMemory("BIOS.z80");
		machine.loadIntelHexIntoMemory("bdos.ihx", 0);
		machine.loadIntoMemory("Boot.z80");

		for (int drive = 0; drive < DRIVE_COUNT; drive++) {
			String imageName = Paths.get(diskDir, "z80-disk" + drive + ".img").toString();
			try {
				// Created fresh (with a valid header) on first run if it doesn't exist yet.
				machine.attachDrive(drive, imageName);
			} catch (Exception e) {
				logger.error("Unable to mount " + imageName + " - drive " + drive + " will report not-ready", e);
			}
		}

		Z80InstructionDecoder decoder = new Z80InstructionDecoder();
		FullInstructionSet.populate(decoder);
		Z80CPU cpu = new Z80CPU(machine.memoryBus(), machine.portBus(), decoder);
		cpu.pc = Z80BinaryFile.read("Boot.z80").startAddress;

		machine.video().show();

		Thread cpuThread = new Thread(() -> runLoop(cpu), "Z80CPU");
		cpuThread.setDaemon(true);
		cpuThread.start();
	}

	private static void runLoop(Z80CPU cpu) {
		while (true) {
			try {
				cpu.serviceInterrupts();
				cpu.step();
			} catch (Exception e) {
				logger.error("Z80 CPU halted", e);
				break;
			}
		}
	}
}
