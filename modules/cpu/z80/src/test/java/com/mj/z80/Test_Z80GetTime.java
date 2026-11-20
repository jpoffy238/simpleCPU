package com.mj.z80;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.mj.z80.instructions.FullInstructionSet;

/** Boots the real assembled GetTimeTest.z80 (src/main/asm) through a real
 * Z80VideoDiskMachine - proves BIOS.asm's new BIOS_GET_TIME primitive works
 * end to end through a real BIOS call and a real RTC device, the same role
 * Test_Z80DiskPrimitives/Test_Z80GraphicsPrimitives already play for the
 * disk/graphics primitives. */
public class Test_Z80GetTime {
	private Z80VideoDiskMachine machine;
	private Z80CPU cpu;

	@BeforeEach
	public void setup() throws Exception {
		machine = new Z80VideoDiskMachine();
		machine.loadIntoMemory("BIOS.z80");
		machine.loadIntoMemory("GetTimeTest.z80");

		Z80InstructionDecoder decoder = new Z80InstructionDecoder();
		FullInstructionSet.populate(decoder);
		cpu = new Z80CPU(machine.memoryBus(), machine.portBus(), decoder);
		cpu.pc = Z80BinaryFile.read("GetTimeTest.z80").startAddress;
	}

	/** Runs until the CPU settles into GetTimeTest.asm's own terminal `DONE:
	 * jp DONE` self-loop (three consecutive identical PC values) - same
	 * technique Test_Z80DiskPrimitives/Test_Z80GraphicsPrimitives already
	 * use. */
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
		throw new AssertionError("GetTimeTest.asm never reached its own DONE loop within " + maxSteps + " steps");
	}

	@Test
	public void bootedProgramCanReadTheRealTimeThroughTheBiosGetTimePrimitive() throws Exception {
		long before = System.currentTimeMillis() / 1000L;
		runToCompletion(10_000);
		long after = System.currentTimeMillis() / 1000L;

		long epoch = 0;
		for (int i = 3; i >= 0; i--) {
			epoch = (epoch << 8) | (machine.memoryBus().read(0x2000 + i) & 0xffL);
		}
		assertTrue(epoch >= before && epoch <= after,
				"BIOS_GET_TIME result " + epoch + " should be between " + before + " and " + after);
	}
}
