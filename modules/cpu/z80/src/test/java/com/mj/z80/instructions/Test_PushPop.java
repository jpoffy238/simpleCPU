package com.mj.z80.instructions;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.mj.z80.StackRegisterPair;
import com.mj.z80.Z80CPU;
import com.mj.z80.Z80InstructionDecoder;
import com.mj.z80.Z80Machine;

/** PUSH qq/POP qq and PUSH/POP IX/IY (see PushPopTable). PUSH BC/POP BC
 * are pinned against their real, independently-known opcodes ($C5/$C1);
 * the round-trip sweep covers DE/HL/AF, and dedicated tests cover IX/IY -
 * same reasoning as every other family's pinned-plus-sweep pattern. */
public class Test_PushPop {
	private Z80Machine machine;
	private Z80CPU cpu;

	@BeforeEach
	public void setup() {
		machine = new Z80Machine();
		Z80InstructionDecoder decoder = new Z80InstructionDecoder();
		PushPopTable.populate(decoder);
		cpu = new Z80CPU(machine.memoryBus(), machine.portBus(), decoder);
	}

	private void loadBytes(int... bytes) throws Exception {
		for (int i = 0; i < bytes.length; i++) {
			machine.memoryBus().write(0x1000 + i, (byte) bytes[i]);
		}
		cpu.pc = 0x1000;
	}

	@Test
	public void pushBcWritesHighByteThenLowByteAndDecrementsSpByTwo() throws Exception {
		cpu.bc.set16(0x1234);
		cpu.sp = 0x9000;
		loadBytes(0xC5);

		cpu.step();

		assertEquals(0x8FFE, cpu.sp);
		assertEquals(0x34, machine.memoryBus().read(0x8FFE) & 0xff, "low byte at the final SP");
		assertEquals(0x12, machine.memoryBus().read(0x8FFF) & 0xff, "high byte one above it");
		assertEquals(0x1001, cpu.pc);
	}

	@Test
	public void popBcIsTheExactInverseOfPush() throws Exception {
		cpu.sp = 0x8FFE;
		machine.memoryBus().write(0x8FFE, (byte) 0x34);
		machine.memoryBus().write(0x8FFF, (byte) 0x12);
		loadBytes(0xC1);

		cpu.step();

		assertEquals(0x1234, cpu.bc.get16());
		assertEquals(0x9000, cpu.sp);
		assertEquals(0x1001, cpu.pc);
	}

	@Test
	public void pushThenPopRoundTripsForEveryStackRegisterPair() throws Exception {
		for (StackRegisterPair rr : StackRegisterPair.values()) {
			cpu.sp = 0x9000;
			rr.get(cpu).set16(0xABCD);
			loadBytes(0xC5 | (rr.code() << 4));
			cpu.step();

			rr.get(cpu).set16(0x0000);
			loadBytes(0xC1 | (rr.code() << 4));
			cpu.step();

			assertEquals(0xABCD, rr.get(cpu).get16(), rr + " round trip");
			assertEquals(0x9000, cpu.sp, rr + " SP back to where it started");
		}
	}

	@Test
	public void pushIxThenPopIxRoundTrips() throws Exception {
		cpu.sp = 0x9000;
		cpu.ix.set16(0x5678);
		loadBytes(0xDD, 0xE5);

		cpu.step();

		assertEquals(0x8FFE, cpu.sp);
		assertEquals(0x1002, cpu.pc, "prefix + opcode = 2 bytes consumed");

		cpu.ix.set16(0x0000);
		loadBytes(0xDD, 0xE1);

		cpu.step();

		assertEquals(0x5678, cpu.ix.get16());
		assertEquals(0x9000, cpu.sp);
	}

	@Test
	public void pushIyThenPopIyRoundTrips() throws Exception {
		cpu.sp = 0x9000;
		cpu.iy.set16(0x9ABC);
		loadBytes(0xFD, 0xE5);

		cpu.step();

		cpu.iy.set16(0x0000);
		loadBytes(0xFD, 0xE1);

		cpu.step();

		assertEquals(0x9ABC, cpu.iy.get16());
		assertEquals(0x9000, cpu.sp);
	}

	@Test
	public void pushAfThenPopAfMovesFsRawBitsRatherThanInterpretingThem() throws Exception {
		cpu.sp = 0x9000;
		cpu.af.set16(0x1234); // A=$12, F=$34 - raw bytes, no real flag meaning needed for this test
		loadBytes(0xF5); // PUSH AF

		cpu.step();

		cpu.af.set16(0x0000);
		loadBytes(0xF1); // POP AF

		cpu.step();

		assertEquals(0x1234, cpu.af.get16(), "PUSH/POP AF moves F's raw bits exactly like any other register pair's low byte");
	}
}
