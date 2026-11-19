package com.mj.z80;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.BooleanSupplier;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.mj.z80.instructions.FullInstructionSet;

/** Boots the real SDCC-compiled hello.ihx (src/main/c, shared hello.c/bios.h
 * with the 6502 side - see modules/cpu/c-shared) through a real
 * Z80VideoDiskMachine, proving the whole SDCC-C-to-running-Z80-program
 * pipeline end to end, the same role Test_Z80Boot already proves for
 * hand-written Demo.asm. hello.ihx isn't part of the Maven build (SDCC's
 * src/main/c Makefile is a manual step, same convention Test_CCP/Test_Edit
 * already use on the 6502 side) - this test skips rather than fails if it
 * hasn't been built yet. */
public class Test_Z80HelloWorld {
	private Z80VideoDiskMachine machine;
	private Z80CPU cpu;

	private static Path helloIhx() {
		return Paths.get("src/main/c/hello.ihx");
	}

	@BeforeEach
	public void setup() throws Exception {
		Assumptions.assumeTrue(Files.exists(helloIhx()),
				"hello.ihx not built - run `make` in modules/cpu/z80/src/main/c first");

		machine = new Z80VideoDiskMachine();
		machine.loadIntoMemory("BIOS.z80");
		machine.loadIntelHexIntoMemory(helloIhx().toString(), 0);

		Z80InstructionDecoder decoder = new Z80InstructionDecoder();
		FullInstructionSet.populate(decoder);
		cpu = new Z80CPU(machine.memoryBus(), machine.portBus(), decoder);
		// NOT --code-loc (0x1000) - unlike hand-written Demo.asm (which has no
		// crt0 and really does start at its own org), SDCC's own crt0 relies on
		// the classic Z80 convention that power-on starts execution at address
		// 0 (the reset vector SDCC's linker places there: "JP $0100" into the
		// real crt0, which sets SP, zeroes BSS, then CALLs _main). Found the
		// hard way: booting straight at 0x1000 landed mid-function (SDCC's own
		// default exit-trap stub, not _main), corrupting SP via a stray RETI.
		cpu.pc = 0x0000;
	}

	private void runUntil(BooleanSupplier condition, int maxSteps) throws Exception {
		for (int i = 0; i < maxSteps && !condition.getAsBoolean(); i++) {
			cpu.serviceInterrupts();
			cpu.step();
		}
		assertTrue(condition.getAsBoolean(), "condition never became true within " + maxSteps + " steps");
	}

	private int peek(int address) {
		try {
			return machine.memoryBus().read(address) & 0xff;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private static final String GREETING = "Hello";

	@Test
	public void sdccCompiledProgramPrintsItsGreetingIntoVideoRam() throws Exception {
		runUntil(() -> peek(0xb800 + GREETING.length() - 1) == GREETING.charAt(GREETING.length() - 1), 200_000);

		for (int i = 0; i < GREETING.length(); i++) {
			assertEquals(GREETING.charAt(i), peek(0xb800 + i), "mismatch at column " + i);
		}
	}
}
