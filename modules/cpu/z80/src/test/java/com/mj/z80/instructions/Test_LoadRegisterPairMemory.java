package com.mj.z80.instructions;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.mj.z80.RegisterPair16;
import com.mj.z80.Z80CPU;
import com.mj.z80.Z80InstructionDecoder;
import com.mj.z80.Z80Machine;

/** LD HL/IX/IY,(nn), LD (nn),HL/IX/IY, the ED-prefixed LD dd,(nn)/LD
 * (nn),dd family, and LD SP,HL/IX/IY (see LoadRegisterPairMemoryTable).
 * Each direct form is exercised individually (rather than folded into a
 * sweep) since HL/IX/IY/BC/DE/SP all go through genuinely different
 * opcode bytes and tables here, not one formula the way Register8 or
 * RegisterPair16 produce theirs - a sweep would just be these same
 * assertions with extra indirection. */
public class Test_LoadRegisterPairMemory {
	private Z80Machine machine;
	private Z80CPU cpu;

	@BeforeEach
	public void setup() {
		machine = new Z80Machine();
		Z80InstructionDecoder decoder = new Z80InstructionDecoder();
		LoadRegisterPairMemoryTable.populate(decoder);
		cpu = new Z80CPU(machine.memoryBus(), machine.portBus(), decoder);
	}

	private void loadBytes(int... bytes) throws Exception {
		for (int i = 0; i < bytes.length; i++) {
			machine.memoryBus().write(0x1000 + i, (byte) bytes[i]);
		}
		cpu.pc = 0x1000;
	}

	@Test
	public void ldHlFromMemory() throws Exception {
		machine.memoryBus().write(0x5000, (byte) 0x34);
		machine.memoryBus().write(0x5001, (byte) 0x12);
		cpu.af.setLo(0x55); // flags sentinel - LD must never touch it
		loadBytes(0x2A, 0x00, 0x50);

		cpu.step();

		assertEquals(0x1234, cpu.hl.get16());
		assertEquals(0x55, cpu.af.lo(), "LD HL,(nn) must never affect flags");
		assertEquals(0x1003, cpu.pc);
	}

	@Test
	public void storeHlToMemory() throws Exception {
		cpu.hl.set16(0x1234);
		loadBytes(0x22, 0x00, 0x50);

		cpu.step();

		assertEquals(0x34, machine.memoryBus().read(0x5000) & 0xff);
		assertEquals(0x12, machine.memoryBus().read(0x5001) & 0xff);
		assertEquals(0x1003, cpu.pc);
	}

	@Test
	public void ldIxFromMemory() throws Exception {
		machine.memoryBus().write(0x5100, (byte) 0x78);
		machine.memoryBus().write(0x5101, (byte) 0x56);
		loadBytes(0xDD, 0x2A, 0x00, 0x51);

		cpu.step();

		assertEquals(0x5678, cpu.ix.get16());
		assertEquals(0x1004, cpu.pc, "prefix + opcode + 2 address bytes = 4 bytes consumed");
	}

	@Test
	public void storeIxToMemory() throws Exception {
		cpu.ix.set16(0x5678);
		loadBytes(0xDD, 0x22, 0x00, 0x51);

		cpu.step();

		assertEquals(0x78, machine.memoryBus().read(0x5100) & 0xff);
		assertEquals(0x56, machine.memoryBus().read(0x5101) & 0xff);
		assertEquals(0x1004, cpu.pc);
	}

	@Test
	public void ldIyFromMemory() throws Exception {
		machine.memoryBus().write(0x5200, (byte) 0x9A);
		machine.memoryBus().write(0x5201, (byte) 0xBC);
		loadBytes(0xFD, 0x2A, 0x00, 0x52);

		cpu.step();

		assertEquals(0xBC9A, cpu.iy.get16());
		assertEquals(0x1004, cpu.pc);
	}

	@Test
	public void storeIyToMemory() throws Exception {
		cpu.iy.set16(0xBC9A);
		loadBytes(0xFD, 0x22, 0x00, 0x52);

		cpu.step();

		assertEquals(0x9A, machine.memoryBus().read(0x5200) & 0xff);
		assertEquals(0xBC, machine.memoryBus().read(0x5201) & 0xff);
		assertEquals(0x1004, cpu.pc);
	}

	@Test
	public void edLoadBcFromMemory() throws Exception {
		machine.memoryBus().write(0x5300, (byte) 0x11);
		machine.memoryBus().write(0x5301, (byte) 0x22);
		loadBytes(0xED, 0x4B, 0x00, 0x53);

		cpu.step();

		assertEquals(0x2211, cpu.bc.get16());
		assertEquals(0x1004, cpu.pc, "ED-prefixed form: prefix + opcode + 2 address bytes = 4 bytes consumed");
	}

	@Test
	public void edStoreBcToMemory() throws Exception {
		cpu.bc.set16(0x2211);
		loadBytes(0xED, 0x43, 0x00, 0x53);

		cpu.step();

		assertEquals(0x11, machine.memoryBus().read(0x5300) & 0xff);
		assertEquals(0x22, machine.memoryBus().read(0x5301) & 0xff);
		assertEquals(0x1004, cpu.pc);
	}

	@Test
	public void edLoadSpFromMemory() throws Exception {
		machine.memoryBus().write(0x5400, (byte) 0x00);
		machine.memoryBus().write(0x5401, (byte) 0x90);
		loadBytes(0xED, 0x7B, 0x00, 0x54);

		cpu.step();

		assertEquals(0x9000, cpu.sp, "LD SP,(nn) must reach the plain-int sp field, not a RegisterPair");
		assertEquals(0x1004, cpu.pc);
	}

	@Test
	public void edStoreSpToMemory() throws Exception {
		cpu.sp = 0x9000;
		loadBytes(0xED, 0x73, 0x00, 0x54);

		cpu.step();

		assertEquals(0x00, machine.memoryBus().read(0x5400) & 0xff);
		assertEquals(0x90, machine.memoryBus().read(0x5401) & 0xff);
		assertEquals(0x1004, cpu.pc);
	}

	/** Every RegisterPair16 (including SP) round-tripping through the
	 * ED-prefixed family's own formula - the 4 explicit tests above pin
	 * BC and SP against real opcodes; this sweep is the regression net for
	 * DE and HL's slots (and re-confirms BC/SP) without hand-writing a
	 * separate explicit test per register. */
	@Test
	public void everyRegisterPairRoundTripsThroughDirectMemoryViaEd() throws Exception {
		for (RegisterPair16 rr : RegisterPair16.values()) {
			rr.set16(cpu, 0xABCD);
			loadBytes(0xED, 0x43 | (rr.code() << 4), 0x00, 0x60);
			cpu.step();
			assertEquals(0xCD, machine.memoryBus().read(0x6000) & 0xff, rr + " store, low byte");
			assertEquals(0xAB, machine.memoryBus().read(0x6001) & 0xff, rr + " store, high byte");

			rr.set16(cpu, 0x0000);
			loadBytes(0xED, 0x4B | (rr.code() << 4), 0x00, 0x60);
			cpu.step();
			assertEquals(0xABCD, rr.get16(cpu), rr + " load");
		}
	}

	@Test
	public void ldSpFromHl() throws Exception {
		cpu.hl.set16(0x7000);
		loadBytes(0xF9);

		cpu.step();

		assertEquals(0x7000, cpu.sp);
		assertEquals(0x1001, cpu.pc);
	}

	@Test
	public void ldSpFromIx() throws Exception {
		cpu.ix.set16(0x7100);
		loadBytes(0xDD, 0xF9);

		cpu.step();

		assertEquals(0x7100, cpu.sp);
		assertEquals(0x1002, cpu.pc);
	}

	@Test
	public void ldSpFromIy() throws Exception {
		cpu.iy.set16(0x7200);
		loadBytes(0xFD, 0xF9);

		cpu.step();

		assertEquals(0x7200, cpu.sp);
		assertEquals(0x1002, cpu.pc);
	}
}
