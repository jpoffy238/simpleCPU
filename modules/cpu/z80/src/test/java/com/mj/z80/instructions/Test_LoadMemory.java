package com.mj.z80.instructions;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.mj.z80.Register8;
import com.mj.z80.Z80CPU;
import com.mj.z80.Z80InstructionDecoder;
import com.mj.z80.Z80Machine;

/** LD r,(HL) / LD (HL),r / LD (HL),n - every plain-HL memory-indirect form
 * (see LoadMemoryTable). LD A,(HL) ($7E), LD (HL),A ($77), and LD (HL),n
 * ($36) are pinned explicitly against their real, independently-known
 * opcodes; the sweep tests cover the register-to-register-shaped rest -
 * including LD H,(HL)/LD L,(HL) and LD (HL),H/LD (HL),L, where the
 * register under test is also part of the address being read/written, a
 * real legal case that needs its own care in test setup (see the ordering
 * notes below) even though it's nothing special on real hardware - HL is
 * read to form the address before any register write happens. */
public class Test_LoadMemory {
	private Z80Machine machine;
	private Z80CPU cpu;

	@BeforeEach
	public void setup() {
		machine = new Z80Machine();
		Z80InstructionDecoder decoder = new Z80InstructionDecoder();
		LoadMemoryTable.populate(decoder);
		cpu = new Z80CPU(machine.memoryBus(), machine.portBus(), decoder);
	}

	private void assertLoadsFromMemory(int opcode, Register8 destination) throws Exception {
		// destination is set, THEN hl - if destination happens to be H or L,
		// this ordering keeps hl.set16() as the authoritative final address
		// rather than letting an earlier destination.set() clobber half of
		// it after the fact.
		destination.set(cpu, 0x00);
		cpu.hl.set16(0x3000);
		machine.memoryBus().write(0x3000, (byte) 0x99);
		cpu.af.setLo(0x55); // flags sentinel - LD must never touch it
		machine.memoryBus().write(0x1000, (byte) opcode);
		cpu.pc = 0x1000;

		cpu.step();

		assertEquals(0x99, destination.get(cpu), "destination should hold the byte at [HL]");
		assertEquals(0x99, machine.memoryBus().read(0x3000) & 0xff, "memory itself must be untouched");
		assertEquals(0x55, cpu.af.lo(), "LD r,(HL) must never affect flags");
		assertEquals(0x1001, cpu.pc);
	}

	private void assertStoresToMemory(int opcode, Register8 source) throws Exception {
		cpu.hl.set16(0x3100);
		// When source IS H or L, it's unavoidably also half of the address -
		// there's no separate "source value" to force in that case, so the
		// expected byte is just whatever hl.set16() already put there,
		// rather than an arbitrary sentinel every other register gets.
		if (source != Register8.H && source != Register8.L) {
			source.set(cpu, 0x77);
		}
		int expected = source.get(cpu);
		machine.memoryBus().write(0x3100, (byte) 0x00);
		cpu.af.setLo(0x55);
		machine.memoryBus().write(0x1000, (byte) opcode);
		cpu.pc = 0x1000;

		cpu.step();

		assertEquals(expected, machine.memoryBus().read(0x3100) & 0xff, "memory at [HL] should hold the source register's value");
		assertEquals(expected, source.get(cpu), "the source register itself must be left untouched");
		assertEquals(0x55, cpu.af.lo(), "LD (HL),r must never affect flags");
		assertEquals(0x1001, cpu.pc);
	}

	@Test
	public void ldAFromHl() throws Exception {
		assertLoadsFromMemory(0x7E, Register8.A);
	}

	@Test
	public void ldHlFromA() throws Exception {
		assertStoresToMemory(0x77, Register8.A);
	}

	@Test
	public void ldHlFromImmediate() throws Exception {
		cpu.hl.set16(0x3200);
		machine.memoryBus().write(0x3200, (byte) 0x00);
		cpu.af.setLo(0x55); // flags sentinel - LD must never touch it
		machine.memoryBus().write(0x1000, (byte) 0x36);
		machine.memoryBus().write(0x1001, (byte) 0x99);
		cpu.pc = 0x1000;

		cpu.step();

		assertEquals(0x99, machine.memoryBus().read(0x3200) & 0xff, "memory at [HL] should hold the literal byte that followed the opcode");
		assertEquals(0x55, cpu.af.lo(), "LD (HL),n must never affect flags");
		assertEquals(0x1002, cpu.pc, "opcode + immediate byte = 2 bytes consumed");
	}

	@Test
	public void everyRegisterLoadsFromHl() throws Exception {
		for (Register8 r : Register8.values()) {
			assertLoadsFromMemory(0x40 | (r.code() << 3) | 0x06, r);
		}
	}

	@Test
	public void everyRegisterStoresToHl() throws Exception {
		for (Register8 r : Register8.values()) {
			assertStoresToMemory(0x70 | r.code(), r);
		}
	}
}
