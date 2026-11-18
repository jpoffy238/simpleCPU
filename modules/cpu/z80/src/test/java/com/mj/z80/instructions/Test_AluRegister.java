package com.mj.z80.instructions;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.mj.z80.Flags;
import com.mj.z80.Register8;
import com.mj.z80.Z80CPU;
import com.mj.z80.Z80InstructionDecoder;
import com.mj.z80.Z80Machine;

/** ADD A,r / ADC A,r / SUB r / SBC A,r / AND r / XOR r / OR r / CP r (see
 * AluRegisterTable). The first instruction family where flags actually
 * get computed, so each operation gets its own hand-verified cases
 * (chosen to isolate S/Z/H/P-V/N/C one at a time, not just "it ran"),
 * using opcode $8x-$BFx with Register8.C ($81/$89/.../$B9) as the
 * generic "some register" throughout. One dispatch sweep (ADD across
 * every register) confirms the decoder wiring itself - which register
 * actually got read - separately from the flag math, which the per-
 * operation cases already pin. */
public class Test_AluRegister {
	private Z80Machine machine;
	private Z80CPU cpu;

	@BeforeEach
	public void setup() {
		machine = new Z80Machine();
		Z80InstructionDecoder decoder = new Z80InstructionDecoder();
		AluRegisterTable.populate(decoder);
		cpu = new Z80CPU(machine.memoryBus(), machine.portBus(), decoder);
	}

	private void run(int opcode) throws Exception {
		machine.memoryBus().write(0x1000, (byte) opcode);
		cpu.pc = 0x1000;
		cpu.step();
	}

	// ---- ADD A,r ($80-$87; C is $81) ----

	@Test
	public void addSimpleCaseSetsNoFlags() throws Exception {
		cpu.af.setHi(0x02);
		cpu.bc.setLo(0x03);

		run(0x81); // ADD A,C

		assertEquals(0x05, cpu.af.hi());
		assertEquals(0, cpu.af.lo());
	}

	@Test
	public void addIgnoresAnyExistingCarryFlag() throws Exception {
		cpu.af.setHi(0x02);
		cpu.bc.setLo(0x03);
		cpu.af.setLo(Flags.C); // pre-set, must NOT be added in - that's ADC's job

		run(0x81);

		assertEquals(0x05, cpu.af.hi(), "ADD must not consult the carry flag");
	}

	@Test
	public void addSetsCarryHalfCarryAndZeroOnWraparound() throws Exception {
		cpu.af.setHi(0xFF);
		cpu.bc.setLo(0x01);

		run(0x81);

		assertEquals(0x00, cpu.af.hi());
		assertEquals(Flags.Z | Flags.H | Flags.C, cpu.af.lo());
	}

	@Test
	public void addSetsHalfCarryAloneAcrossTheNibbleBoundary() throws Exception {
		cpu.af.setHi(0x0F);
		cpu.bc.setLo(0x01);

		run(0x81);

		assertEquals(0x10, cpu.af.hi());
		assertEquals(Flags.H, cpu.af.lo());
	}

	@Test
	public void addSetsSignHalfCarryAndOverflowCrossingIntoNegative() throws Exception {
		cpu.af.setHi(0x7F);
		cpu.bc.setLo(0x01);

		run(0x81);

		assertEquals(0x80, cpu.af.hi());
		assertEquals(Flags.S | Flags.H | Flags.PV, cpu.af.lo());
	}

	// ---- ADC A,r ($88-$8F; C is $89) ----

	@Test
	public void adcAddsTheCarryInOnTopOfTheOperands() throws Exception {
		cpu.af.setHi(0x00);
		cpu.bc.setLo(0x00);
		cpu.af.setLo(Flags.C);

		run(0x89); // ADC A,C

		assertEquals(0x01, cpu.af.hi(), "0 + 0 + carry-in(1) = 1, unlike plain ADD");
		assertEquals(0, cpu.af.lo());
	}

	@Test
	public void adcCarryInCanPushAResultThatAddAloneWouldNotReach() throws Exception {
		cpu.af.setHi(0xFE);
		cpu.bc.setLo(0x01);
		cpu.af.setLo(Flags.C);

		run(0x89);

		assertEquals(0x00, cpu.af.hi(), "0xFE + 1 + carry-in(1) wraps to 0 - plain ADD (0xFE+1=0xFF) would not");
		assertEquals(Flags.Z | Flags.H | Flags.C, cpu.af.lo());
	}

	// ---- SUB r ($90-$97; C is $91) ----

	@Test
	public void subSimpleCaseSetsOnlyN() throws Exception {
		cpu.af.setHi(0x05);
		cpu.bc.setLo(0x03);

		run(0x91); // SUB C

		assertEquals(0x02, cpu.af.hi());
		assertEquals(Flags.N, cpu.af.lo(), "N is always set for a subtract, even one with no borrow");
	}

	@Test
	public void subSetsSignHalfCarryNAndCarryOnBorrow() throws Exception {
		cpu.af.setHi(0x03);
		cpu.bc.setLo(0x05);

		run(0x91);

		assertEquals(0xFE, cpu.af.hi());
		assertEquals(Flags.S | Flags.H | Flags.N | Flags.C, cpu.af.lo());
	}

	@Test
	public void subDetectsOverflowWhenOperandsHaveDifferentSigns() throws Exception {
		cpu.af.setHi(0x80); // -128
		cpu.bc.setLo(0x01); // +1 - 128 - 1 = -129, outside signed 8-bit range

		run(0x91);

		assertEquals(0x7F, cpu.af.hi());
		assertEquals(Flags.H | Flags.PV | Flags.N, cpu.af.lo(), "no borrow needed (128-1 unsigned is fine), but signed overflow occurred");
	}

	// ---- SBC A,r ($98-$9F; C is $99) ----

	@Test
	public void sbcSubtractsTheCarryInOnTopOfTheOperand() throws Exception {
		cpu.af.setHi(0x00);
		cpu.bc.setLo(0x00);
		cpu.af.setLo(Flags.C);

		run(0x99); // SBC A,C

		assertEquals(0xFF, cpu.af.hi(), "0 - 0 - carry-in(1) borrows to 0xFF, unlike plain SUB (0-0=0)");
		assertEquals(Flags.S | Flags.H | Flags.N | Flags.C, cpu.af.lo());
	}

	// ---- CP r ($B8-$BF; C is $B9) ----

	@Test
	public void cpComputesFlagsLikeSubButNeverWritesBackToA() throws Exception {
		cpu.af.setHi(0x05);
		cpu.bc.setLo(0x05);

		run(0xB9); // CP C

		assertEquals(0x05, cpu.af.hi(), "A must be unchanged - CP only ever sets flags");
		assertEquals(Flags.Z | Flags.N, cpu.af.lo(), "same flags SUB would set for 5-5");
	}

	// ---- AND r ($A0-$A7; C is $A1) ----

	@Test
	public void andAlwaysSetsHalfCarryEvenThoughNothingCarries() throws Exception {
		cpu.af.setHi(0x0C); // 1100
		cpu.bc.setLo(0x0A); // 1010

		run(0xA1); // AND C

		assertEquals(0x08, cpu.af.hi()); // 1000
		assertEquals(Flags.H, cpu.af.lo(), "H=1 is AND's own documented quirk - PV clear: 0x08 has odd parity");
	}

	@Test
	public void andSetsParityForAnEvenNumberOfOneBits() throws Exception {
		cpu.af.setHi(0xFF);
		cpu.bc.setLo(0x0F);

		run(0xA1);

		assertEquals(0x0F, cpu.af.hi());
		assertEquals(Flags.H | Flags.PV, cpu.af.lo(), "0x0F = 00001111 has 4 (even) one-bits");
	}

	// ---- OR r ($B0-$B7; C is $B1) ----

	@Test
	public void orClearsHalfCarryUnlikeAnd() throws Exception {
		cpu.af.setHi(0x00);
		cpu.bc.setLo(0x00);

		run(0xB1); // OR C

		assertEquals(0x00, cpu.af.hi());
		assertEquals(Flags.Z | Flags.PV, cpu.af.lo(), "H clear (OR's own rule); zero has even (0) parity, so PV is set alongside Z");
	}

	// ---- XOR r ($A8-$AF; C is $A9) ----

	@Test
	public void xorOfAValueWithItselfIsTheClassicZeroIdiom() throws Exception {
		cpu.af.setHi(0xFF);
		cpu.bc.setLo(0xFF);

		run(0xA9); // XOR C

		assertEquals(0x00, cpu.af.hi());
		assertEquals(Flags.Z | Flags.PV, cpu.af.lo());
	}

	@Test
	public void xorSetsSignAndParityWithoutZero() throws Exception {
		cpu.af.setHi(0xFF);
		cpu.bc.setLo(0x00);

		run(0xA9);

		assertEquals(0xFF, cpu.af.hi());
		assertEquals(Flags.S | Flags.PV, cpu.af.lo(), "0xFF = 8 (even) one-bits");
	}

	// ---- dispatch sweep: does every opcode slot read the right register? ----

	@Test
	public void addDispatchesToEveryRegisterCorrectly() throws Exception {
		for (Register8 r : Register8.values()) {
			machine = new Z80Machine();
			Z80InstructionDecoder decoder = new Z80InstructionDecoder();
			AluRegisterTable.populate(decoder);
			cpu = new Z80CPU(machine.memoryBus(), machine.portBus(), decoder);

			// Set r THEN A, not the other way around - when r is A itself,
			// this ordering (same trick Test_LoadMemory uses for its own
			// H/L-is-also-the-address case) leaves A authoritatively at
			// 0x10 rather than whichever call happened to run last by
			// accident, and reading r back afterward (rather than assuming
			// a fixed operand) gives the right expectation either way:
			// "ADD A,A" doubles A, every other register just adds 0x03.
			r.set(cpu, 0x03);
			cpu.af.setHi(0x10);
			int expected = (0x10 + r.get(cpu)) & 0xff;
			int opcode = 0x80 | r.code(); // ADD A,r

			run(opcode);

			assertEquals(expected, cpu.af.hi(), "opcode $" + Integer.toHexString(opcode) + " (ADD A," + r + ")");
		}
	}
}
