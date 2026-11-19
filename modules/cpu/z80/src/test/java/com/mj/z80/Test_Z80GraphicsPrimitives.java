package com.mj.z80;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.mj.z80.instructions.FullInstructionSet;

/** Boots the real assembled GraphicsTest.z80 (src/main/asm) through a
 * real Z80VideoDiskMachine - proves BIOS.asm's new graphics primitives
 * (BIOS_GRAPHICS_MODE/BIOS_SET_PIXEL) work end to end through real BIOS
 * calls, the same role Test_Z80DiskPrimitives already plays for the disk
 * primitives, and the same boundary cases Test_BIOS's own graphics test
 * covers on the 6502 side. */
public class Test_Z80GraphicsPrimitives {
	private Z80VideoDiskMachine machine;
	private Z80CPU cpu;

	@BeforeEach
	public void setup() throws Exception {
		machine = new Z80VideoDiskMachine();
		machine.loadIntoMemory("BIOS.z80");
		machine.loadIntoMemory("GraphicsTest.z80");

		Z80InstructionDecoder decoder = new Z80InstructionDecoder();
		FullInstructionSet.populate(decoder);
		cpu = new Z80CPU(machine.memoryBus(), machine.portBus(), decoder);
		cpu.pc = Z80BinaryFile.read("GraphicsTest.z80").startAddress;
	}

	@Test
	public void bootedProgramCanDrawAndClearPixelsThroughTheGraphicsPrimitives() throws Exception {
		int lastPc = -1;
		int repeatCount = 0;
		for (int i = 0; i < 200_000; i++) {
			int pc = cpu.pc;
			if (pc == lastPc && ++repeatCount == 3) {
				break;
			}
			if (pc != lastPc) {
				repeatCount = 0;
			}
			lastPc = pc;
			cpu.serviceInterrupts();
			cpu.step();
		}

		assertTrue(machine.video().isGraphicsMode(),
				"BIOS_GRAPHICS_MODE should have switched VideoController into graphics mode");
		assertTrue(machine.video().getPixel(0, 0), "(0,0) should be on");
		assertTrue(machine.video().getPixel(7, 0), "(7,0), the other end of the same byte, should be on");
		assertTrue(machine.video().getPixel(319, 199), "(319,199), the bottom-right corner, should be on");
		assertFalse(machine.video().getPixel(3, 2), "(3,2) was set then cleared - should be off");
	}
}
