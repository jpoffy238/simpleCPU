package com.mj.z80.instructions;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.mj.z80.IndexRegister;
import com.mj.z80.Register8;
import com.mj.z80.RegisterPair;
import com.mj.z80.Z80CPU;
import com.mj.z80.Z80InstructionDecoder;
import com.mj.z80.Z80Machine;

/** LD r,(IX+d) / LD (IX+d),r / LD (IX+d),n and their IY counterparts (see
 * LoadIndexedTable). Unlike the plain-HL family, r here (even H or L) is
 * never entangled with the addressing - IX/IY only replaces HL in the
 * addressing role, so test setup needs none of Test_LoadMemory's special
 * casing. LD A,(IX+d)/(IY+d), LD (IX+d)/(IY+d),A, and LD (IX+d)/(IY+d),n
 * are pinned against their real, independently-known opcodes; the sweeps
 * cover the rest, and a dedicated test confirms a negative displacement
 * actually wraps the address backward rather than just happening to work
 * for the sweep's one fixed positive offset. */
public class Test_LoadIndexed {
	private Z80Machine machine;
	private Z80CPU cpu;

	@BeforeEach
	public void setup() {
		machine = new Z80Machine();
		Z80InstructionDecoder decoder = new Z80InstructionDecoder();
		LoadIndexedTable.populate(decoder);
		cpu = new Z80CPU(machine.memoryBus(), machine.portBus(), decoder);
	}

	private int prefixOf(IndexRegister index) {
		return index == IndexRegister.IX ? 0xDD : 0xFD;
	}

	private RegisterPair registerOf(IndexRegister index) {
		return index == IndexRegister.IX ? cpu.ix : cpu.iy;
	}

	private void assertLoadsFromIndexed(IndexRegister index, int opcode, Register8 destination) throws Exception {
		destination.set(cpu, 0x00);
		registerOf(index).set16(0x4000);
		machine.memoryBus().write(0x4005, (byte) 0x99); // IX/IY + displacement 5
		cpu.af.setLo(0x55); // flags sentinel - LD must never touch it
		machine.memoryBus().write(0x1000, (byte) prefixOf(index));
		machine.memoryBus().write(0x1001, (byte) opcode);
		machine.memoryBus().write(0x1002, (byte) 0x05);
		cpu.pc = 0x1000;

		cpu.step();

		assertEquals(0x99, destination.get(cpu), "destination should hold the byte at [index+d]");
		assertEquals(0x55, cpu.af.lo(), "LD r,(I+d) must never affect flags");
		assertEquals(0x1003, cpu.pc, "prefix + opcode + displacement = 3 bytes consumed");
	}

	private void assertStoresToIndexed(IndexRegister index, int opcode, Register8 source) throws Exception {
		registerOf(index).set16(0x4100);
		source.set(cpu, 0x77);
		machine.memoryBus().write(0x4105, (byte) 0x00);
		cpu.af.setLo(0x55);
		machine.memoryBus().write(0x1000, (byte) prefixOf(index));
		machine.memoryBus().write(0x1001, (byte) opcode);
		machine.memoryBus().write(0x1002, (byte) 0x05);
		cpu.pc = 0x1000;

		cpu.step();

		assertEquals(0x77, machine.memoryBus().read(0x4105) & 0xff, "memory at [index+d] should hold the source register's value");
		assertEquals(0x77, source.get(cpu), "the source register itself must be left untouched");
		assertEquals(0x55, cpu.af.lo(), "LD (I+d),r must never affect flags");
		assertEquals(0x1003, cpu.pc);
	}

	private void assertStoresImmediateToIndexed(IndexRegister index) throws Exception {
		registerOf(index).set16(0x4200);
		machine.memoryBus().write(0x4205, (byte) 0x00);
		cpu.af.setLo(0x55);
		machine.memoryBus().write(0x1000, (byte) prefixOf(index));
		machine.memoryBus().write(0x1001, (byte) 0x36);
		machine.memoryBus().write(0x1002, (byte) 0x05);
		machine.memoryBus().write(0x1003, (byte) 0x99);
		cpu.pc = 0x1000;

		cpu.step();

		assertEquals(0x99, machine.memoryBus().read(0x4205) & 0xff, "memory at [index+d] should hold the literal byte");
		assertEquals(0x55, cpu.af.lo(), "LD (I+d),n must never affect flags");
		assertEquals(0x1004, cpu.pc, "prefix + opcode + displacement + immediate = 4 bytes consumed");
	}

	@Test
	public void ldAFromIxPlusD() throws Exception {
		assertLoadsFromIndexed(IndexRegister.IX, 0x7E, Register8.A);
	}

	@Test
	public void ldIxPlusDFromA() throws Exception {
		assertStoresToIndexed(IndexRegister.IX, 0x77, Register8.A);
	}

	@Test
	public void ldIxPlusDFromImmediate() throws Exception {
		assertStoresImmediateToIndexed(IndexRegister.IX);
	}

	@Test
	public void ldAFromIyPlusD() throws Exception {
		assertLoadsFromIndexed(IndexRegister.IY, 0x7E, Register8.A);
	}

	@Test
	public void ldIyPlusDFromA() throws Exception {
		assertStoresToIndexed(IndexRegister.IY, 0x77, Register8.A);
	}

	@Test
	public void ldIyPlusDFromImmediate() throws Exception {
		assertStoresImmediateToIndexed(IndexRegister.IY);
	}

	@Test
	public void negativeDisplacementWrapsTheAddressBackward() throws Exception {
		cpu.ix.set16(0x4000);
		machine.memoryBus().write(0x3FFF, (byte) 0x42); // IX + (-1)
		machine.memoryBus().write(0x1000, (byte) 0xDD);
		machine.memoryBus().write(0x1001, (byte) 0x7E); // LD A,(IX+d)
		machine.memoryBus().write(0x1002, (byte) 0xFF); // d = -1
		cpu.pc = 0x1000;

		cpu.step();

		assertEquals(0x42, cpu.af.hi(), "displacement must sign-extend, not be treated as +255");
	}

	@Test
	public void everyRegisterLoadsFromEitherIndexRegister() throws Exception {
		for (IndexRegister index : IndexRegister.values()) {
			for (Register8 r : Register8.values()) {
				assertLoadsFromIndexed(index, 0x40 | (r.code() << 3) | 0x06, r);
			}
		}
	}

	@Test
	public void everyRegisterStoresToEitherIndexRegister() throws Exception {
		for (IndexRegister index : IndexRegister.values()) {
			for (Register8 r : Register8.values()) {
				assertStoresToIndexed(index, 0x70 | r.code(), r);
			}
		}
	}
}
