package com.mj.z80.instructions;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.mj.z80.Flags;
import com.mj.z80.Register8;
import com.mj.z80.Z80CPU;
import com.mj.z80.Z80InstructionDecoder;
import com.mj.z80.Z80Machine;

/** INC r / DEC r (see IncDecRegisterTable). The real quirk worth pinning
 * carefully here: the carry flag is never touched by either, unlike
 * every ALU operation - tested explicitly in both directions (C
 * preserved as 1, and preserved as 0) rather than just assumed from the
 * production code reading the same way. Uses opcode $0C/$0D (INC C/DEC
 * C) as the generic case throughout; a dispatch sweep covers every other
 * register. */
public class Test_IncDecRegister {
	private Z80Machine machine;
	private Z80CPU cpu;

	@BeforeEach
	public void setup() {
		machine = new Z80Machine();
		Z80InstructionDecoder decoder = new Z80InstructionDecoder();
		IncDecRegisterTable.populate(decoder);
		cpu = new Z80CPU(machine.memoryBus(), machine.portBus(), decoder);
	}

	private void run(int opcode) throws Exception {
		machine.memoryBus().write(0x1000, (byte) opcode);
		cpu.pc = 0x1000;
		cpu.step();
	}

	// ---- INC r ($0C is INC C) ----

	@Test
	public void incSimpleCaseSetsNoFlags() throws Exception {
		cpu.bc.setLo(0x00);
		cpu.af.setLo(0x00);

		run(0x0C);

		assertEquals(0x01, cpu.bc.lo());
		assertEquals(0, cpu.af.lo());
	}

	@Test
	public void incSetsHalfCarryAcrossTheNibbleBoundary() throws Exception {
		cpu.bc.setLo(0x0F);

		run(0x0C);

		assertEquals(0x10, cpu.bc.lo());
		assertEquals(Flags.H, cpu.af.lo());
	}

	@Test
	public void incSetsSignHalfCarryAndOverflowCrossingIntoNegative() throws Exception {
		cpu.bc.setLo(0x7F);

		run(0x0C);

		assertEquals(0x80, cpu.bc.lo());
		assertEquals(Flags.S | Flags.H | Flags.PV, cpu.af.lo());
	}

	@Test
	public void incSetsZeroAndHalfCarryOnWraparoundButNeverCarry() throws Exception {
		cpu.bc.setLo(0xFF);

		run(0x0C);

		assertEquals(0x00, cpu.bc.lo());
		assertEquals(Flags.Z | Flags.H, cpu.af.lo(), "C must stay clear even though the byte wrapped");
	}

	@Test
	public void incPreservesAnExistingCarryFlagRatherThanClearingOrSettingIt() throws Exception {
		cpu.bc.setLo(0x00);
		cpu.af.setLo(Flags.C);

		run(0x0C);

		assertEquals(Flags.C, cpu.af.lo(), "carry must be left exactly as it was");
	}

	// ---- DEC r ($0D is DEC C) ----

	@Test
	public void decSimpleCaseSetsOnlyN() throws Exception {
		cpu.bc.setLo(0x05);

		run(0x0D);

		assertEquals(0x04, cpu.bc.lo());
		assertEquals(Flags.N, cpu.af.lo(), "N is always set for a decrement, even one with no borrow");
	}

	@Test
	public void decSetsHalfCarryAndNAcrossTheNibbleBoundary() throws Exception {
		cpu.bc.setLo(0x10);

		run(0x0D);

		assertEquals(0x0F, cpu.bc.lo());
		assertEquals(Flags.H | Flags.N, cpu.af.lo());
	}

	@Test
	public void decSetsHalfCarryOverflowAndNCrossingIntoPositive() throws Exception {
		cpu.bc.setLo(0x80);

		run(0x0D);

		assertEquals(0x7F, cpu.bc.lo());
		assertEquals(Flags.H | Flags.PV | Flags.N, cpu.af.lo(), "S clear - 0x7F's own sign bit is 0");
	}

	@Test
	public void decSetsSignHalfCarryAndNOnWraparoundButNeverCarry() throws Exception {
		cpu.bc.setLo(0x00);

		run(0x0D);

		assertEquals(0xFF, cpu.bc.lo());
		assertEquals(Flags.S | Flags.H | Flags.N, cpu.af.lo(), "C must stay clear even though the byte wrapped");
	}

	@Test
	public void decPreservesAnExistingCarryFlagRatherThanClearingOrSettingIt() throws Exception {
		cpu.bc.setLo(0x01);
		cpu.af.setLo(Flags.C);

		run(0x0D);

		assertEquals(Flags.C | Flags.Z | Flags.N, cpu.af.lo(), "carry must be left exactly as it was");
	}

	// ---- dispatch sweep ----

	@Test
	public void incDispatchesToEveryRegisterCorrectly() throws Exception {
		for (Register8 r : Register8.values()) {
			setup();
			r.set(cpu, 0x10);
			int opcode = 0x04 | (r.code() << 3);

			run(opcode);

			assertEquals(0x11, r.get(cpu), "opcode $" + Integer.toHexString(opcode) + " (INC " + r + ")");
		}
	}
}
