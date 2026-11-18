package com.mj.z80.instructions;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.mj.z80.RegisterPair16;
import com.mj.z80.Z80CPU;
import com.mj.z80.Z80InstructionDecoder;
import com.mj.z80.Z80Machine;

/** LD dd,nn (BC/DE/HL/SP) and LD IX,nn/LD IY,nn (see
 * LoadRegisterPairImmediateTable). LD BC,nn is pinned against its real,
 * independently-known opcode; the sweep covers the rest of the base-table
 * family. */
public class Test_LoadRegisterPairImmediate {
	private Z80Machine machine;
	private Z80CPU cpu;

	@BeforeEach
	public void setup() {
		machine = new Z80Machine();
		Z80InstructionDecoder decoder = new Z80InstructionDecoder();
		LoadRegisterPairImmediateTable.populate(decoder);
		cpu = new Z80CPU(machine.memoryBus(), machine.portBus(), decoder);
	}

	private void loadBytes(int... bytes) throws Exception {
		for (int i = 0; i < bytes.length; i++) {
			machine.memoryBus().write(0x1000 + i, (byte) bytes[i]);
		}
		cpu.pc = 0x1000;
	}

	@Test
	public void ldBcNn() throws Exception {
		cpu.af.setLo(0x55); // flags sentinel - LD must never touch it
		loadBytes(0x01, 0x34, 0x12);

		cpu.step();

		assertEquals(0x1234, cpu.bc.get16(), "low byte first, high byte second");
		assertEquals(0x55, cpu.af.lo(), "LD dd,nn must never affect flags");
		assertEquals(0x1003, cpu.pc, "opcode + 2 immediate bytes = 3 bytes consumed");
	}

	@Test
	public void everyRegisterPairLoadsItsOwnImmediate() throws Exception {
		for (RegisterPair16 rr : RegisterPair16.values()) {
			int opcode = 0x01 | (rr.code() << 4);
			loadBytes(opcode, 0x78, 0x56);

			cpu.step();

			assertEquals(0x5678, rr.get16(cpu), "opcode $" + Integer.toHexString(opcode));
			assertEquals(0x1003, cpu.pc);
		}
	}

	@Test
	public void ldIxNn() throws Exception {
		loadBytes(0xDD, 0x21, 0x99, 0x88);

		cpu.step();

		assertEquals(0x8899, cpu.ix.get16());
		assertEquals(0x1004, cpu.pc, "prefix + opcode + 2 immediate bytes = 4 bytes consumed");
	}

	@Test
	public void ldIyNn() throws Exception {
		loadBytes(0xFD, 0x21, 0x11, 0x22);

		cpu.step();

		assertEquals(0x2211, cpu.iy.get16());
		assertEquals(0x1004, cpu.pc);
	}
}
