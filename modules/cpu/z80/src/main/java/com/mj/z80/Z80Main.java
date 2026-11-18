package com.mj.z80;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.mj.z80.instructions.FullInstructionSet;

/** Boots Z80VideoDiskMachine: loads BIOS.z80 (console primitives, see
 * src/main/asm/BIOS.asm) and Demo.z80 (the first real program, see
 * src/main/asm/Demo.asm) onto the memory bus, opens the video window,
 * and runs the CPU on its own thread - Z80CPU itself stays a plain,
 * synchronous object (used directly, no threading concerns, by every
 * instruction test), so the threading lives here instead, the same
 * reason the 6502 side's own CPU class extends Thread, just kept out of
 * Z80CPU itself since nothing about instruction execution needs it. A
 * dedicated thread (rather than just looping on the main thread) is what
 * keeps Swing's own event thread free to deliver keystrokes and repaint
 * the window while the CPU runs - BIOS_GETCH's blocking poll would
 * otherwise deadlock against a Swing thread that never gets to run. */
public class Z80Main {
	private static final Logger logger = LogManager.getLogger(Z80Main.class);

	public static void main(String[] args) throws Exception {
		Z80VideoDiskMachine machine = new Z80VideoDiskMachine();
		machine.loadIntoMemory("BIOS.z80");
		machine.loadIntoMemory("Demo.z80");

		Z80InstructionDecoder decoder = new Z80InstructionDecoder();
		FullInstructionSet.populate(decoder);
		Z80CPU cpu = new Z80CPU(machine.memoryBus(), machine.portBus(), decoder);
		cpu.pc = Z80BinaryFile.read("Demo.z80").startAddress;

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
