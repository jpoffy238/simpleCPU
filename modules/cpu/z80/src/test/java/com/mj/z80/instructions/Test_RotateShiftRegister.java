package com.mj.z80.instructions;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.mj.z80.Flags;
import com.mj.z80.Register8;
import com.mj.z80.Z80CPU;
import com.mj.z80.Z80InstructionDecoder;
import com.mj.z80.Z80Machine;

/** RLC/RRC/RL/RR/SLA/SRA/SRL r (see RotateShiftTable). Each operation
 * gets a hand-verified case isolating its own distinguishing behavior
 * (RLC/RRC's self-wrap vs RL/RR's carry-in, SLA/SRL's 0-fill vs SRA's
 * sign-preserve), using opcode $CB $x1 (register C) throughout; a
 * dispatch sweep covers every other register. */
public class Test_RotateShiftRegister {
	private Z80Machine machine;
	private Z80CPU cpu;

	@BeforeEach
	public void setup() {
		machine = new Z80Machine();
		Z80InstructionDecoder decoder = new Z80InstructionDecoder();
		RotateShiftTable.populate(decoder);
		cpu = new Z80CPU(machine.memoryBus(), machine.portBus(), decoder);
	}

	private void run(int cbOpcode) throws Exception {
		machine.memoryBus().write(0x1000, (byte) 0xCB);
		machine.memoryBus().write(0x1001, (byte) cbOpcode);
		cpu.pc = 0x1000;
		cpu.step();
	}

	@Test
	public void rlcWrapsBit7AroundToBit0AndCarry() throws Exception {
		cpu.bc.setLo(0x80);

		run(0x01); // RLC C

		assertEquals(0x01, cpu.bc.lo());
		assertEquals(Flags.C, cpu.af.lo());
		assertEquals(0x1002, cpu.pc, "prefix + opcode = 2 bytes consumed");
	}

	@Test
	public void rrcWrapsBit0AroundToBit7AndCarry() throws Exception {
		cpu.bc.setLo(0x01);

		run(0x09); // RRC C

		assertEquals(0x80, cpu.bc.lo());
		assertEquals(Flags.S | Flags.C, cpu.af.lo());
	}

	@Test
	public void rlRoutesTheExistingCarryIntoBit0RatherThanSelfWrapping() throws Exception {
		cpu.bc.setLo(0x80);
		cpu.af.setLo(0x00); // carry-in clear

		run(0x11); // RL C

		assertEquals(0x00, cpu.bc.lo(), "bit 0 comes from carry-in (0), not from bit 7 wrapping around");
		assertEquals(Flags.Z | Flags.PV | Flags.C, cpu.af.lo());
	}

	@Test
	public void rrRoutesTheExistingCarryIntoBit7RatherThanSelfWrapping() throws Exception {
		cpu.bc.setLo(0x01);
		cpu.af.setLo(0x00); // carry-in clear

		run(0x19); // RR C

		assertEquals(0x00, cpu.bc.lo(), "bit 7 comes from carry-in (0), not from bit 0 wrapping around");
		assertEquals(Flags.Z | Flags.PV | Flags.C, cpu.af.lo());
	}

	@Test
	public void slaShiftsAZeroIntoBit0() throws Exception {
		cpu.bc.setLo(0x40);

		run(0x21); // SLA C

		assertEquals(0x80, cpu.bc.lo());
		assertEquals(Flags.S, cpu.af.lo());
	}

	@Test
	public void sraPreservesTheSignBitRatherThanShiftingAZeroIn() throws Exception {
		cpu.bc.setLo(0x80);

		run(0x29); // SRA C

		assertEquals(0xC0, cpu.bc.lo(), "bit 7 (the sign) copies down, unlike SRL");
		assertEquals(Flags.S | Flags.PV, cpu.af.lo());
	}

	@Test
	public void srlShiftsAZeroIntoBit7UnlikeSra() throws Exception {
		cpu.bc.setLo(0x80);

		run(0x39); // SRL C

		assertEquals(0x40, cpu.bc.lo(), "bit 7 becomes 0, not a copy of the old bit 7");
		assertEquals(0, cpu.af.lo());
	}

	@Test
	public void rlcDispatchesToEveryRegisterCorrectly() throws Exception {
		for (Register8 r : Register8.values()) {
			setup();
			r.set(cpu, 0x80);
			int opcode = r.code(); // RLC r, op code 0

			run(opcode);

			assertEquals(0x01, r.get(cpu), "opcode $CB $" + Integer.toHexString(opcode) + " (RLC " + r + ")");
		}
	}
}
