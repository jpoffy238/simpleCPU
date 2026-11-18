package com.mj.z80.instructions;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.mj.z80.RegisterPair16;
import com.mj.z80.Z80CPU;
import com.mj.z80.Z80InstructionDecoder;
import com.mj.z80.Z80Machine;

/** INC/DEC BC/DE/HL/SP and INC/DEC IX/IY (see IncDecRegisterPairTable).
 * The real quirk worth pinning carefully here: 16-bit INC/DEC affects NO
 * flag at all, unlike the 8-bit family - tested with a flags sentinel
 * across the wraparound cases specifically, where it would be easiest to
 * accidentally wire in Z/C from the 8-bit math. */
public class Test_IncDecRegisterPair {
	private Z80Machine machine;
	private Z80CPU cpu;

	@BeforeEach
	public void setup() {
		machine = new Z80Machine();
		Z80InstructionDecoder decoder = new Z80InstructionDecoder();
		IncDecRegisterPairTable.populate(decoder);
		cpu = new Z80CPU(machine.memoryBus(), machine.portBus(), decoder);
	}

	private void loadBytes(int... bytes) throws Exception {
		for (int i = 0; i < bytes.length; i++) {
			machine.memoryBus().write(0x1000 + i, (byte) bytes[i]);
		}
		cpu.pc = 0x1000;
	}

	@Test
	public void incBcIncrementsAndAffectsNoFlag() throws Exception {
		cpu.bc.set16(0x1233);
		cpu.af.setLo(0x55); // flags sentinel

		loadBytes(0x03); // INC BC
		cpu.step();

		assertEquals(0x1234, cpu.bc.get16());
		assertEquals(0x55, cpu.af.lo(), "16-bit INC must never affect flags");
		assertEquals(0x1001, cpu.pc);
	}

	@Test
	public void decBcDecrementsAndAffectsNoFlag() throws Exception {
		cpu.bc.set16(0x1234);
		cpu.af.setLo(0x55);

		loadBytes(0x0B); // DEC BC
		cpu.step();

		assertEquals(0x1233, cpu.bc.get16());
		assertEquals(0x55, cpu.af.lo(), "16-bit DEC must never affect flags");
		assertEquals(0x1001, cpu.pc);
	}

	@Test
	public void incWrapsFromFfffToZeroWithNoFlagEffect() throws Exception {
		cpu.hl.set16(0xFFFF);
		cpu.af.setLo(0x55);

		loadBytes(0x23); // INC HL
		cpu.step();

		assertEquals(0x0000, cpu.hl.get16());
		assertEquals(0x55, cpu.af.lo(), "wrapping to zero must not set Z or any other flag");
	}

	@Test
	public void decWrapsFromZeroToFfffWithNoFlagEffect() throws Exception {
		cpu.hl.set16(0x0000);
		cpu.af.setLo(0x55);

		loadBytes(0x2B); // DEC HL
		cpu.step();

		assertEquals(0xFFFF, cpu.hl.get16());
		assertEquals(0x55, cpu.af.lo());
	}

	@Test
	public void incDecSpReachesThePlainIntFieldNotARegisterPair() throws Exception {
		cpu.sp = 0x9000;

		loadBytes(0x33); // INC SP
		cpu.step();
		assertEquals(0x9001, cpu.sp);

		loadBytes(0x3B); // DEC SP
		cpu.step();
		assertEquals(0x9000, cpu.sp);
	}

	@Test
	public void everyRegisterPairIncrementsCorrectly() throws Exception {
		for (RegisterPair16 rr : RegisterPair16.values()) {
			setup();
			rr.set16(cpu, 0x1000);
			int opcode = 0x03 | (rr.code() << 4);

			loadBytes(opcode);
			cpu.step();

			assertEquals(0x1001, rr.get16(cpu), "opcode $" + Integer.toHexString(opcode) + " (INC " + rr + ")");
		}
	}

	@Test
	public void incDecIxAndIyRoundTrip() throws Exception {
		cpu.ix.set16(0x5678);
		loadBytes(0xDD, 0x23); // INC IX
		cpu.step();
		assertEquals(0x5679, cpu.ix.get16());

		loadBytes(0xDD, 0x2B); // DEC IX
		cpu.step();
		assertEquals(0x5678, cpu.ix.get16());

		cpu.iy.set16(0x9ABC);
		loadBytes(0xFD, 0x23); // INC IY
		cpu.step();
		assertEquals(0x9ABD, cpu.iy.get16());

		loadBytes(0xFD, 0x2B); // DEC IY
		cpu.step();
		assertEquals(0x9ABC, cpu.iy.get16());
	}
}
