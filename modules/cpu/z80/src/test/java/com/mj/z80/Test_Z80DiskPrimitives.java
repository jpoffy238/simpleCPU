package com.mj.z80;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.mj.z80.instructions.FullInstructionSet;

/** Boots the real assembled DiskTest.z80 (src/main/asm) through a real
 * Z80VideoDiskMachine with a real attached disk - proves BIOS.asm's new
 * disk primitives (BIOS_SELECT/BIOS_READ_SECTOR/BIOS_WRITE_SECTOR) work end
 * to end through real BIOS calls, before BDOS gets built on top of them,
 * the same role Test_Z80Boot already plays for the console primitives. */
public class Test_Z80DiskPrimitives {
	@TempDir
	Path tempDir;

	private Z80VideoDiskMachine machine;
	private Z80CPU cpu;

	@BeforeEach
	public void setup() throws Exception {
		machine = new Z80VideoDiskMachine();
		machine.attachDrive(0, tempDir.resolve("z80-disk0.img").toString());
		machine.loadIntoMemory("BIOS.z80");
		machine.loadIntoMemory("DiskTest.z80");

		Z80InstructionDecoder decoder = new Z80InstructionDecoder();
		FullInstructionSet.populate(decoder);
		cpu = new Z80CPU(machine.memoryBus(), machine.portBus(), decoder);
		cpu.pc = Z80BinaryFile.read("DiskTest.z80").startAddress;
	}

	/** Runs until the CPU settles into DiskTest.asm's own terminal `DONE: jp
	 * DONE` self-loop (three consecutive identical PC values) - more robust
	 * than guessing at which exact step a particular screen byte stops being
	 * a space, since that can't distinguish "not yet printed" from "already
	 * printed, but a later write happens to land on the same cell". By the
	 * time this returns, the whole write/read/verify/print sequence is
	 * guaranteed to have finished one way or the other. */
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
		throw new AssertionError("DiskTest.asm never reached its own DONE loop within " + maxSteps + " steps");
	}

	private int peek(int address) {
		try {
			return machine.memoryBus().read(address) & 0xff;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Test
	public void writeThenReadSectorRoundTripsThroughRealBiosCalls() throws Exception {
		runToCompletion(200_000);

		assertEquals('O', peek(0xb800), "expected BIOS_READ_SECTOR to read back exactly what BIOS_WRITE_SECTOR wrote");
		assertEquals('K', peek(0xb801));
	}
}
