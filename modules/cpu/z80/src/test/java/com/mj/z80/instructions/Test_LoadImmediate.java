package com.mj.z80.instructions;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.mj.z80.Register8;
import com.mj.z80.Z80CPU;
import com.mj.z80.Z80InstructionDecoder;
import com.mj.z80.Z80Machine;

/** LD r,n - immediate 8-bit loads (see LoadImmediateTable). A couple of
 * real, independently-known opcodes are pinned explicitly (LD A,n and LD
 * B,n - likely the two most commonly seen Z80 opcodes of all) as a
 * cross-check against LoadImmediateTable's own formula, same reasoning as
 * Test_LoadRegister's pinned checks; the sweep test covers the rest. */
public class Test_LoadImmediate {
	private Z80Machine machine;
	private Z80CPU cpu;

	@BeforeEach
	public void setup() {
		machine = new Z80Machine();
		Z80InstructionDecoder decoder = new Z80InstructionDecoder();
		LoadImmediateTable.populate(decoder);
		cpu = new Z80CPU(machine.memoryBus(), machine.portBus(), decoder);
	}

	private void assertLoadsImmediate(int opcode, Register8 destination) throws Exception {
		destination.set(cpu, 0x00);
		cpu.af.setLo(0x55); // flags sentinel - LD must never touch it
		machine.memoryBus().write(0x1000, (byte) opcode);
		machine.memoryBus().write(0x1001, (byte) 0x99);
		cpu.pc = 0x1000;

		cpu.step();

		assertEquals(0x99, destination.get(cpu), "destination should hold the literal byte that followed the opcode");
		assertEquals(0x55, cpu.af.lo(), "LD r,n must never affect flags");
		assertEquals(0x1002, cpu.pc, "opcode + immediate byte = 2 bytes consumed");
	}

	@Test
	public void ldAN() throws Exception {
		assertLoadsImmediate(0x3E, Register8.A);
	}

	@Test
	public void ldBN() throws Exception {
		assertLoadsImmediate(0x06, Register8.B);
	}

	@Test
	public void everyRegisterLoadsItsOwnImmediateByte() throws Exception {
		for (Register8 destination : Register8.values()) {
			int opcode = 0x06 | (destination.code() << 3);
			assertLoadsImmediate(opcode, destination);
		}
	}
}
