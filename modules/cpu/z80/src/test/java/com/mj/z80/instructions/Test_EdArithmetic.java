package com.mj.z80.instructions;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.mj.z80.Flags;
import com.mj.z80.RegisterPair16;
import com.mj.z80.Z80CPU;
import com.mj.z80.Z80InstructionDecoder;
import com.mj.z80.Z80Machine;

/** NEG, ADC HL,rr, and SBC HL,rr (see EdArithmeticTable) - the first
 * ED-prefixed instructions beyond LD's own slice. ADC/SBC HL,rr are the
 * first 16-bit operations that actually compute flags (unlike the
 * unprefixed 16-bit INC/DEC/ADD HL,rr family, which don't). */
public class Test_EdArithmetic {
	private Z80Machine machine;
	private Z80CPU cpu;

	@BeforeEach
	public void setup() {
		machine = new Z80Machine();
		Z80InstructionDecoder decoder = new Z80InstructionDecoder();
		EdArithmeticTable.populate(decoder);
		cpu = new Z80CPU(machine.memoryBus(), machine.portBus(), decoder);
	}

	private void run(int edOpcode) throws Exception {
		machine.memoryBus().write(0x1000, (byte) 0xED);
		machine.memoryBus().write(0x1001, (byte) edOpcode);
		cpu.pc = 0x1000;
		cpu.step();
	}

	// ---- NEG ----

	@Test
	public void negSimpleCase() throws Exception {
		cpu.af.setHi(0x01);

		run(0x44);

		assertEquals(0xFF, cpu.af.hi());
		assertEquals(Flags.S | Flags.H | Flags.N | Flags.C, cpu.af.lo());
		assertEquals(0x1002, cpu.pc, "prefix + opcode = 2 bytes consumed");
	}

	@Test
	public void negOfZeroStaysZeroWithNoCarry() throws Exception {
		cpu.af.setHi(0x00);

		run(0x44);

		assertEquals(0x00, cpu.af.hi());
		assertEquals(Flags.Z | Flags.N, cpu.af.lo(), "0 - 0 needs no borrow, unlike every other value");
	}

	@Test
	public void negOfNegative128OverflowsAndStaysTheSame() throws Exception {
		cpu.af.setHi(0x80);

		run(0x44);

		assertEquals(0x80, cpu.af.hi(), "-128 has no positive counterpart in signed 8-bit, so it negates to itself");
		assertEquals(Flags.S | Flags.PV | Flags.N | Flags.C, cpu.af.lo());
	}

	// ---- ADC HL,rr ($4A is ADC HL,BC) ----

	@Test
	public void adcHlAddsTheCarryInOnTopOfBothOperands() throws Exception {
		cpu.hl.set16(0x0001);
		cpu.bc.set16(0x0001);
		cpu.af.setLo(Flags.C);

		run(0x4A); // ADC HL,BC

		assertEquals(0x0003, cpu.hl.get16(), "1 + 1 + carry-in(1) = 3");
	}

	@Test
	public void adcHlSetsZeroHalfCarryAndCarryOnWraparound() throws Exception {
		cpu.hl.set16(0xFFFF);
		cpu.bc.set16(0x0000);
		cpu.af.setLo(Flags.C);

		run(0x4A); // ADC HL,BC

		assertEquals(0x0000, cpu.hl.get16());
		assertEquals(Flags.Z | Flags.H | Flags.C, cpu.af.lo());
	}

	@Test
	public void adcHlDetectsOverflowCrossingIntoNegative() throws Exception {
		cpu.hl.set16(0x7FFF);
		cpu.bc.set16(0x0000);
		cpu.af.setLo(Flags.C);

		run(0x4A); // ADC HL,BC

		assertEquals(0x8000, cpu.hl.get16());
		assertEquals(Flags.S | Flags.H | Flags.PV, cpu.af.lo());
	}

	// ---- SBC HL,rr ($42 is SBC HL,BC) ----

	@Test
	public void sbcHlSubtractsTheCarryInOnTopOfBothOperands() throws Exception {
		cpu.hl.set16(0x0000);
		cpu.bc.set16(0x0000);
		cpu.af.setLo(Flags.C);

		run(0x42); // SBC HL,BC

		assertEquals(0xFFFF, cpu.hl.get16(), "0 - 0 - carry-in(1) borrows to 0xFFFF, unlike a plain SUB with no carry");
		assertEquals(Flags.S | Flags.H | Flags.N | Flags.C, cpu.af.lo());
	}

	// ---- dispatch sweeps ----

	@Test
	public void adcHlDispatchesToEveryRegisterPairCorrectly() throws Exception {
		for (RegisterPair16 rr : RegisterPair16.values()) {
			setup();
			cpu.hl.set16(0x1000);
			rr.set16(cpu, rr == RegisterPair16.HL ? 0x1000 : 0x0010);
			int opcode = 0x4A | (rr.code() << 4);

			run(opcode);

			int expected = rr == RegisterPair16.HL ? 0x2000 : 0x1010;
			assertEquals(expected, cpu.hl.get16(), "opcode $ED $" + Integer.toHexString(opcode) + " (ADC HL," + rr + ")");
		}
	}

	@Test
	public void sbcHlDispatchesToEveryRegisterPairCorrectly() throws Exception {
		for (RegisterPair16 rr : RegisterPair16.values()) {
			setup();
			cpu.hl.set16(0x2000);
			if (rr != RegisterPair16.HL) {
				rr.set16(cpu, 0x0010);
			}
			int opcode = 0x42 | (rr.code() << 4);

			run(opcode);

			int expected = rr == RegisterPair16.HL ? 0x0000 : 0x1FF0;
			assertEquals(expected, cpu.hl.get16(), "opcode $ED $" + Integer.toHexString(opcode) + " (SBC HL," + rr + ")");
		}
	}
}
