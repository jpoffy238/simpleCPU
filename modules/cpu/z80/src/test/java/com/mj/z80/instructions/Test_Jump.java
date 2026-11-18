package com.mj.z80.instructions;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.mj.z80.Condition;
import com.mj.z80.Flags;
import com.mj.z80.Z80CPU;
import com.mj.z80.Z80InstructionDecoder;
import com.mj.z80.Z80Machine;

/** JP nn / JP cc,nn / JP (HL)/(IX)/(IY) (see JumpTable). */
public class Test_Jump {
	private Z80Machine machine;
	private Z80CPU cpu;

	@BeforeEach
	public void setup() {
		machine = new Z80Machine();
		Z80InstructionDecoder decoder = new Z80InstructionDecoder();
		JumpTable.populate(decoder);
		cpu = new Z80CPU(machine.memoryBus(), machine.portBus(), decoder);
	}

	private void loadBytes(int... bytes) throws Exception {
		for (int i = 0; i < bytes.length; i++) {
			machine.memoryBus().write(0x1000 + i, (byte) bytes[i]);
		}
		cpu.pc = 0x1000;
	}

	@Test
	public void jpNnJumpsUnconditionally() throws Exception {
		loadBytes(0xC3, 0x34, 0x12);

		cpu.step();

		assertEquals(0x1234, cpu.pc);
	}

	@Test
	public void jpZTakenWhenZeroFlagIsSet() throws Exception {
		cpu.af.setLo(Flags.Z);
		loadBytes(0xCA, 0x34, 0x12); // JP Z,nn

		cpu.step();

		assertEquals(0x1234, cpu.pc);
	}

	@Test
	public void jpZStillConsumesTheAddressBytesWhenNotTaken() throws Exception {
		cpu.af.setLo(0x00); // Z clear
		loadBytes(0xCA, 0x34, 0x12); // JP Z,nn

		cpu.step();

		assertEquals(0x1003, cpu.pc, "instruction length doesn't change just because the jump wasn't taken");
	}

	/** NZ/NC/PO/P are satisfied by a CLEARED bit, not a set one - there's no
	 * single flag byte that satisfies all 8 conditions at once, since
	 * they're four complementary pairs. Each condition gets its own
	 * precisely-targeted satisfying/non-satisfying byte instead. */
	private int flagsThatSatisfy(Condition cc) {
		switch (cc) {
		case Z:
			return Flags.Z;
		case C:
			return Flags.C;
		case PE:
			return Flags.PV;
		case M:
			return Flags.S;
		default:
			return 0; // NZ/NC/PO/P are satisfied by that same bit being clear
		}
	}

	@Test
	public void everyConditionGatesTheJumpCorrectly() throws Exception {
		for (Condition cc : Condition.values()) {
			int satisfy = flagsThatSatisfy(cc);
			int doNotSatisfy = ~satisfy & (Flags.Z | Flags.C | Flags.PV | Flags.S);

			setup();
			cpu.af.setLo(satisfy);
			loadBytes(0xC2 | (cc.code() << 3), 0x34, 0x12);
			cpu.step();
			assertEquals(0x1234, cpu.pc, cc + " should be met by flags " + Integer.toBinaryString(satisfy));

			setup();
			cpu.af.setLo(doNotSatisfy);
			loadBytes(0xC2 | (cc.code() << 3), 0x34, 0x12);
			cpu.step();
			assertEquals(0x1003, cpu.pc, cc + " should not be met by flags " + Integer.toBinaryString(doNotSatisfy));
		}
	}

	@Test
	public void jpHlCopiesTheRegisterValueDirectlyRatherThanReadingMemory() throws Exception {
		cpu.hl.set16(0x5678);
		// Deliberately leave 0x5678 in memory un-set (default 0) - if this
		// were actually dereferencing memory, pc would end up 0, not 0x5678.
		loadBytes(0xE9);

		cpu.step();

		assertEquals(0x5678, cpu.pc);
	}

	@Test
	public void jpIxCopiesTheRegisterValueDirectly() throws Exception {
		cpu.ix.set16(0x6789);
		loadBytes(0xDD, 0xE9);

		cpu.step();

		assertEquals(0x6789, cpu.pc);
	}

	@Test
	public void jpIyCopiesTheRegisterValueDirectly() throws Exception {
		cpu.iy.set16(0x789A);
		loadBytes(0xFD, 0xE9);

		cpu.step();

		assertEquals(0x789A, cpu.pc);
	}
}
