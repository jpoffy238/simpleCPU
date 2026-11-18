package com.mj.z80;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.function.BooleanSupplier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.mj.z80.instructions.FullInstructionSet;

/** Boots the real assembled BIOS.z80 (console primitives) and Demo.z80
 * (the first real program - see src/main/asm) through a real
 * Z80VideoDiskMachine, decoder, and CPU - the same end-to-end proof
 * Test_BIOS gives the 6502 side, just headless (VideoController.type()
 * stands in for a keystroke, since there's no window in a test run). */
public class Test_Z80Boot {
	private Z80VideoDiskMachine machine;
	private Z80CPU cpu;

	@BeforeEach
	public void setup() throws Exception {
		machine = new Z80VideoDiskMachine();
		machine.loadIntoMemory("BIOS.z80");
		machine.loadIntoMemory("Demo.z80");

		Z80InstructionDecoder decoder = new Z80InstructionDecoder();
		FullInstructionSet.populate(decoder);
		cpu = new Z80CPU(machine.memoryBus(), machine.portBus(), decoder);
		cpu.pc = Z80BinaryFile.read("Demo.z80").startAddress;
	}

	private void runUntil(BooleanSupplier condition, int maxSteps) throws Exception {
		for (int i = 0; i < maxSteps && !condition.getAsBoolean(); i++) {
			cpu.serviceInterrupts();
			cpu.step();
		}
		assertTrue(condition.getAsBoolean(), "condition never became true within " + maxSteps + " steps");
	}

	/** Plain RAM reads at valid addresses never actually throw - this just
	 * lets the bus's own checked read() be used inside a BooleanSupplier
	 * lambda, which can't declare one. */
	private int peek(int address) {
		try {
			return machine.memoryBus().read(address) & 0xff;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private static final String GREETING = "HELLO FROM THE Z80";

	@Test
	public void demoPrintsItsGreetingIntoVideoRam() throws Exception {
		// Wait for the LAST character, not the first - runUntil returns the
		// instant its condition goes true, which for the first character is
		// long before the rest of the greeting has actually printed.
		runUntil(() -> peek(0xb800 + GREETING.length() - 1) == GREETING.charAt(GREETING.length() - 1), 200_000);

		for (int i = 0; i < GREETING.length(); i++) {
			assertEquals(GREETING.charAt(i), peek(0xb800 + i), "mismatch at column " + i);
		}
	}

	@Test
	public void typingAKeyAfterTheGreetingEchoesItRightAfterTheGreeting() throws Exception {
		runUntil(() -> peek(0xb800 + GREETING.length() - 1) == GREETING.charAt(GREETING.length() - 1), 200_000);
		// Give ECHO_LOOP time to actually process the greeting's own CR/LF
		// and reach its blocking BIOS_GETCH poll before injecting the
		// keystroke, same reasoning VideoController's own type() exists for.
		for (int i = 0; i < 1000; i++) {
			cpu.serviceInterrupts();
			cpu.step();
		}

		machine.video().type("X");

		runUntil(() -> peek(0xb850) == 'X', 200_000);
	}
}
