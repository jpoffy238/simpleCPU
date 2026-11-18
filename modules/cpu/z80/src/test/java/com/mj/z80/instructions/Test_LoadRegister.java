package com.mj.z80.instructions;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.mj.z80.Register8;
import com.mj.z80.Z80CPU;
import com.mj.z80.Z80InstructionDecoder;
import com.mj.z80.Z80Machine;

/** LD A,r - the destination-A slice of the Z80's LD r,r' family (see
 * LoadRegisterTable). Each test loads a single opcode byte, runs it, and
 * checks three things real Z80 hardware guarantees: A takes on the source
 * register's value, the source register itself is left alone (this isn't
 * a swap), and no flag is touched - true of every 8-bit LD, not just
 * these. */
public class Test_LoadRegister {
	private Z80Machine machine;
	private Z80CPU cpu;

	@BeforeEach
	public void setup() {
		machine = new Z80Machine();
		Z80InstructionDecoder decoder = new Z80InstructionDecoder();
		LoadRegisterTable.populate(decoder);
		cpu = new Z80CPU(machine.memoryBus(), machine.portBus(), decoder);
	}

	private void assertLoadsAFromRegister(int opcode, Register8 source) throws Exception {
		cpu.af.setHi(0x00); // A starts different from the value the source is about to get
		source.set(cpu, 0x42);
		cpu.af.setLo(0x55); // flags sentinel - LD must never touch it
		machine.memoryBus().write(0x1000, (byte) opcode);
		cpu.pc = 0x1000;

		cpu.step();

		assertEquals(0x42, cpu.af.hi(), "A should now hold the source register's value");
		assertEquals(0x42, source.get(cpu), "the source register itself must be left untouched - this isn't a swap");
		assertEquals(0x55, cpu.af.lo(), "LD r,r' must never affect flags");
		assertEquals(0x1001, cpu.pc);
	}

	@Test
	public void ldAB() throws Exception {
		assertLoadsAFromRegister(0x78, Register8.B);
	}

	@Test
	public void ldAC() throws Exception {
		assertLoadsAFromRegister(0x79, Register8.C);
	}

	@Test
	public void ldAD() throws Exception {
		assertLoadsAFromRegister(0x7A, Register8.D);
	}

	@Test
	public void ldAE() throws Exception {
		assertLoadsAFromRegister(0x7B, Register8.E);
	}

	@Test
	public void ldAH() throws Exception {
		assertLoadsAFromRegister(0x7C, Register8.H);
	}

	@Test
	public void ldAL() throws Exception {
		assertLoadsAFromRegister(0x7D, Register8.L);
	}

	@Test
	public void ldAAIsARealOpcodeNotJustASkippedSlot() throws Exception {
		assertLoadsAFromRegister(0x7F, Register8.A);
	}
}
