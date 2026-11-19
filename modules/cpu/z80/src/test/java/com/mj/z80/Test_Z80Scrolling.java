package com.mj.z80;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.mj.z80.instructions.FullInstructionSet;

/** Boots the real assembled ScrollTest.z80 (src/main/asm) through a real
 * Z80VideoDiskMachine - proves BIOS.asm's real hardware scrolling
 * (PC_SCROLL, moving Display Start Address rather than copying rows)
 * actually engages and survives wrapping its 2048-byte ring buffer more
 * than once, the same regression Test_BIOS's own
 * printingPastTheBottomScrollsRepeatedlyWithoutFaulting proves on the
 * 6502 side. */
public class Test_Z80Scrolling {
	private Z80VideoDiskMachine machine;
	private Z80CPU cpu;

	private static final int VIDEO_ADDR_PORT = 0x00;
	private static final int VIDEO_DATA_PORT = 0x01;
	private static final int VR_START_ADDR_HI = 12;
	private static final int VR_START_ADDR_LO = 13;

	@BeforeEach
	public void setup() throws Exception {
		machine = new Z80VideoDiskMachine();
		machine.loadIntoMemory("BIOS.z80");
		machine.loadIntoMemory("ScrollTest.z80");

		Z80InstructionDecoder decoder = new Z80InstructionDecoder();
		FullInstructionSet.populate(decoder);
		cpu = new Z80CPU(machine.memoryBus(), machine.portBus(), decoder);
		cpu.pc = Z80BinaryFile.read("ScrollTest.z80").startAddress;
	}

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
		throw new AssertionError("ScrollTest.asm never reached its own DONE loop within " + maxSteps + " steps");
	}

	private int readVideoRegister(int index) throws Exception {
		machine.portBus().write(VIDEO_ADDR_PORT, (byte) index);
		return machine.portBus().read(VIDEO_DATA_PORT) & 0xff;
	}

	@Test
	public void printingPastTheBottomScrollsRepeatedlyWithoutFaulting() throws Exception {
		runToCompletion(2_000_000);

		boolean sawZ = false;
		for (int row = 0; row <= 23; row++) {
			if (machine.video().getRow(row).contains("Z")) {
				sawZ = true;
				break;
			}
		}
		assertTrue(sawZ, "the loop should have run to completion and printed Z without faulting on an unmapped address");

		int startAddress = (readVideoRegister(VR_START_ADDR_HI) << 8) | readVideoRegister(VR_START_ADDR_LO);
		assertTrue(startAddress != 0,
				"Display Start Address should have moved away from 0 - real scrolling should engage the hardware register, not just wrap BIOS's own row counter");
	}
}
