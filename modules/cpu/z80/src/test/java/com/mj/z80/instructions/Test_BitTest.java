package com.mj.z80.instructions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.mj.z80.Flags;
import com.mj.z80.Register8;
import com.mj.z80.Z80CPU;
import com.mj.z80.Z80InstructionDecoder;
import com.mj.z80.Z80Machine;

/** BIT b,r / BIT b,(HL) / BIT b,(IX+d)/(IY+d) (see BitTable). BIT is
 * read-only - never modifies its operand, only sets flags - and has a
 * genuinely easy-to-get-backward flag convention worth pinning
 * explicitly: Z is set when the TESTED BIT IS CLEAR, the opposite sense
 * from most other uses of Z. C and P/V are documented as unaffected/
 * unknown and are left exactly as they were; S only has a defined
 * meaning for bit 7. */
public class Test_BitTest {
	private Z80Machine machine;
	private Z80CPU cpu;

	@BeforeEach
	public void setup() {
		machine = new Z80Machine();
		Z80InstructionDecoder decoder = new Z80InstructionDecoder();
		BitTable.populate(decoder);
		cpu = new Z80CPU(machine.memoryBus(), machine.portBus(), decoder);
	}

	private void run(int cbOpcode) throws Exception {
		machine.memoryBus().write(0x1000, (byte) 0xCB);
		machine.memoryBus().write(0x1001, (byte) cbOpcode);
		cpu.pc = 0x1000;
		cpu.step();
	}

	@Test
	public void zIsSetWhenTheTestedBitIsClear() throws Exception {
		cpu.bc.setLo(0x00);

		run(0x41); // BIT 0,C

		assertEquals(0x00, cpu.bc.lo(), "BIT must never modify its operand");
		assertEquals(Flags.Z | Flags.H, cpu.af.lo());
		assertEquals(0x1002, cpu.pc, "prefix + opcode = 2 bytes consumed");
	}

	@Test
	public void zIsClearWhenTheTestedBitIsSet() throws Exception {
		cpu.bc.setLo(0x01);

		run(0x41); // BIT 0,C

		assertEquals(Flags.H, cpu.af.lo());
	}

	@Test
	public void cAndPvArePreservedRatherThanComputed() throws Exception {
		cpu.bc.setLo(0x01);
		cpu.af.setLo(Flags.C | Flags.PV);

		run(0x41); // BIT 0,C

		assertEquals(Flags.C | Flags.PV | Flags.H, cpu.af.lo(), "C and P/V must survive untouched");
	}

	@Test
	public void sReflectsBit7WhenTestingBit7() throws Exception {
		cpu.bc.setLo(0x80);

		run(0x79); // BIT 7,C

		assertEquals(Flags.S | Flags.H, cpu.af.lo());
	}

	@Test
	public void sIsClearWhenBit7IsClear() throws Exception {
		cpu.bc.setLo(0x00);

		run(0x79); // BIT 7,C

		assertEquals(Flags.Z | Flags.H, cpu.af.lo());
	}

	@Test
	public void bitHlTestsTheByteAtHlWithoutModifyingIt() throws Exception {
		cpu.hl.set16(0x3000);
		machine.memoryBus().write(0x3000, (byte) 0x00);

		run(0x46); // BIT 0,(HL)

		assertEquals(0x00, machine.memoryBus().read(0x3000) & 0xff, "BIT must never modify memory either");
		assertEquals(Flags.Z | Flags.H, cpu.af.lo());
	}

	@Test
	public void bitIxPlusDTestsTheRightByte() throws Exception {
		cpu.ix.set16(0x4000);
		machine.memoryBus().write(0x4005, (byte) 0x01);
		machine.memoryBus().write(0x1000, (byte) 0xDD);
		machine.memoryBus().write(0x1001, (byte) 0xCB);
		machine.memoryBus().write(0x1002, (byte) 0x05);
		machine.memoryBus().write(0x1003, (byte) 0x46); // BIT 0,(IX+5)
		cpu.pc = 0x1000;

		cpu.step();

		assertEquals(Flags.H, cpu.af.lo(), "bit 0 is set, so Z should be clear");
		assertEquals(0x1004, cpu.pc);
	}

	@Test
	public void everyRegisterAndBitDispatchesCorrectly() throws Exception {
		for (Register8 r : Register8.values()) {
			for (int bit = 0; bit < 8; bit++) {
				setup();
				r.set(cpu, 1 << bit); // only that one bit set
				int opcode = 0x40 | (bit << 3) | r.code();

				run(opcode);

				String label = "opcode $CB $" + Integer.toHexString(opcode) + " (BIT " + bit + "," + r + ")";
				assertFalse((cpu.af.lo() & Flags.Z) != 0, label + " - that exact bit is set, so Z should be clear");
			}
		}
	}
}
