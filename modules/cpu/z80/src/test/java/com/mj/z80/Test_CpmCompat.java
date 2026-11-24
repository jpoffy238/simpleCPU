package com.mj.z80;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.mj.z80.instructions.FullInstructionSet;

/** Boots a tiny, hand-assembled fixture (CpmHello.z80) that uses the REAL
 * CP/M 2.2 BDOS calling convention (CALL 5, function number in C) against
 * the new CpmCompat.asm dispatcher - not this project's own custom BIOS_*
 * and BDOS_* ABI that CCP.COM/EDIT.COM use. Phase 1 only: console
 * functions (System Reset/Console Input/Console Output/Print String/Get
 * Console Status/Get Version), delegating into the existing, already-proven
 * BIOS_PUTCH/BIOS_GETCH primitives rather than reimplementing console I/O -
 * see this feature's own planning notes for why. Disk functions are a
 * later, separate phase. */
public class Test_CpmCompat {
	private Z80VideoDiskMachine machine;
	private Z80CPU cpu;

	@BeforeEach
	public void setup() throws Exception {
		machine = new Z80VideoDiskMachine();
		machine.loadIntoMemory("BIOS.z80");
		machine.loadIntoMemory("CpmCompat.z80");
		machine.loadIntoMemory("CpmHello.z80");

		// Real CP/M's own fixed low-memory BDOS entry vector - a real
		// cold-boot loader would poke this once at boot time; this test
		// harness does the same thing by hand, same spirit as every other
		// hand-poked test fixture in this project. $0000 (WBOOT) is left
		// alone - CpmHello.z80 never jumps there.
		machine.memoryBus().write(0x0005, (byte) 0xC3); // JP
		machine.memoryBus().write(0x0006, (byte) 0x00);
		machine.memoryBus().write(0x0007, (byte) 0xE0); // -> $E000 (BDOS_ENTRY)

		Z80InstructionDecoder decoder = new Z80InstructionDecoder();
		FullInstructionSet.populate(decoder);
		cpu = new Z80CPU(machine.memoryBus(), machine.portBus(), decoder);
		cpu.pc = Z80BinaryFile.read("CpmHello.z80").startAddress;
	}

	/** Runs until the CPU settles into CpmHello.z80's own terminal `DONE: jp
	 * DONE` self-loop (three consecutive identical PC values) - same
	 * technique Test_Z80DiskPrimitives/Test_Z80GetTime already use. */
	private void runToCompletion(int maxSteps) throws Exception {
		int lastPc = -1;
		int repeatCount = 0;
		for (int i = 0; i < maxSteps; i++) {
			int pc = cpu.pc;
			if (pc == lastPc && ++repeatCount == 3) {
				return;
			}
			if (pc != lastPc) {
				repeatCount = 0;
			}
			lastPc = pc;
			cpu.serviceInterrupts();
			cpu.step();
		}
		throw new AssertionError("CpmHello.z80 never reached its own DONE loop within " + maxSteps + " steps");
	}

	@Test
	public void realCallFiveFunctionNinePrintsAStringThroughTheExistingBiosPutchPrimitive() throws Exception {
		runToCompletion(10_000);

		assertEquals('H', machine.memoryBus().read(0xb800) & 0xff);
		assertEquals('E', machine.memoryBus().read(0xb801) & 0xff);
		assertEquals('L', machine.memoryBus().read(0xb802) & 0xff);
		assertEquals('L', machine.memoryBus().read(0xb803) & 0xff);
		assertEquals('O', machine.memoryBus().read(0xb804) & 0xff);
	}
}
