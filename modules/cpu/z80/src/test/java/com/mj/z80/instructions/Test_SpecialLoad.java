package com.mj.z80.instructions;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.mj.z80.Z80CPU;
import com.mj.z80.Z80InstructionDecoder;
import com.mj.z80.Z80Machine;

/** LD A,(nn), LD (nn),A, LD I,A, and LD R,A (see SpecialLoadTable). LD A,I
 * and LD A,R are intentionally not tested here - they're not implemented
 * yet (see SpecialLoadTable's class comment on why). */
public class Test_SpecialLoad {
	private Z80Machine machine;
	private Z80CPU cpu;

	@BeforeEach
	public void setup() {
		machine = new Z80Machine();
		Z80InstructionDecoder decoder = new Z80InstructionDecoder();
		SpecialLoadTable.populate(decoder);
		cpu = new Z80CPU(machine.memoryBus(), machine.portBus(), decoder);
	}

	private void loadBytes(int... bytes) throws Exception {
		for (int i = 0; i < bytes.length; i++) {
			machine.memoryBus().write(0x1000 + i, (byte) bytes[i]);
		}
		cpu.pc = 0x1000;
	}

	@Test
	public void ldAFromDirectMemory() throws Exception {
		machine.memoryBus().write(0x8000, (byte) 0x42);
		cpu.af.setLo(0x55); // flags sentinel - LD must never touch it
		loadBytes(0x3A, 0x00, 0x80);

		cpu.step();

		assertEquals(0x42, cpu.af.hi());
		assertEquals(0x55, cpu.af.lo(), "LD A,(nn) must never affect flags");
		assertEquals(0x1003, cpu.pc);
	}

	@Test
	public void storeAToDirectMemory() throws Exception {
		cpu.af.setHi(0x42);
		loadBytes(0x32, 0x00, 0x80);

		cpu.step();

		assertEquals(0x42, machine.memoryBus().read(0x8000) & 0xff);
		assertEquals(0x1003, cpu.pc);
	}

	@Test
	public void ldIFromA() throws Exception {
		cpu.af.setHi(0x37);
		loadBytes(0xED, 0x47);

		cpu.step();

		assertEquals(0x37, cpu.i);
		assertEquals(0x1002, cpu.pc);
	}

	@Test
	public void ldRFromA() throws Exception {
		cpu.af.setHi(0x44);
		loadBytes(0xED, 0x4F);

		cpu.step();

		assertEquals(0x44, cpu.r);
		assertEquals(0x1002, cpu.pc);
	}
}
