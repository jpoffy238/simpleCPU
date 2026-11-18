package com.mj.z80.instructions;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.mj.z80.Flags;
import com.mj.z80.Z80CPU;
import com.mj.z80.Z80InstructionDecoder;
import com.mj.z80.Z80Machine;

/** JR e / JR cc,e / DJNZ e (see JumpRelativeTable). */
public class Test_JumpRelative {
	private Z80Machine machine;
	private Z80CPU cpu;

	@BeforeEach
	public void setup() {
		machine = new Z80Machine();
		Z80InstructionDecoder decoder = new Z80InstructionDecoder();
		JumpRelativeTable.populate(decoder);
		cpu = new Z80CPU(machine.memoryBus(), machine.portBus(), decoder);
	}

	private void loadBytes(int... bytes) throws Exception {
		for (int i = 0; i < bytes.length; i++) {
			machine.memoryBus().write(0x1000 + i, (byte) bytes[i]);
		}
		cpu.pc = 0x1000;
	}

	@Test
	public void jrEJumpsForwardRelativeToTheInstructionAfterIt() throws Exception {
		loadBytes(0x18, 0x05); // JR +5

		cpu.step();

		assertEquals(0x1007, cpu.pc, "0x1002 (past the 2-byte JR) + 5");
	}

	@Test
	public void jrENegativeDisplacementJumpsBackward() throws Exception {
		loadBytes(0x18, 0xFB); // JR -5

		cpu.step();

		assertEquals(0x0FFD, cpu.pc, "0x1002 - 5");
	}

	@Test
	public void jrZTakenWhenZeroFlagSet() throws Exception {
		cpu.af.setLo(Flags.Z);
		loadBytes(0x28, 0x05); // JR Z,+5

		cpu.step();

		assertEquals(0x1007, cpu.pc);
	}

	@Test
	public void jrZStillConsumesTheDisplacementWhenNotTaken() throws Exception {
		cpu.af.setLo(0x00);
		loadBytes(0x28, 0x05); // JR Z,+5

		cpu.step();

		assertEquals(0x1002, cpu.pc, "instruction length doesn't change just because the jump wasn't taken");
	}

	@Test
	public void djnzDecrementsBAndJumpsWhileNonZero() throws Exception {
		cpu.bc.setHi(0x02);
		cpu.af.setLo(0x55); // flags sentinel - DJNZ's own decrement must never touch it
		loadBytes(0x10, 0x05); // DJNZ +5

		cpu.step();

		assertEquals(0x01, cpu.bc.hi(), "B decremented from 2 to 1");
		assertEquals(0x1007, cpu.pc, "1 is non-zero - jump taken");
		assertEquals(0x55, cpu.af.lo(), "DJNZ's own decrement never affects flags, unlike DEC B");
	}

	@Test
	public void djnzDoesNotJumpOnceBReachesZero() throws Exception {
		cpu.bc.setHi(0x01);
		loadBytes(0x10, 0x05); // DJNZ +5

		cpu.step();

		assertEquals(0x00, cpu.bc.hi(), "B decremented from 1 to 0");
		assertEquals(0x1002, cpu.pc, "0 is zero - jump not taken, just past the 2-byte instruction");
	}
}
