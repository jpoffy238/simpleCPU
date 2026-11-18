package com.mj.z80.instructions;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.mj.z80.Flags;
import com.mj.z80.Z80CPU;
import com.mj.z80.Z80InstructionDecoder;
import com.mj.z80.Z80Machine;

/** INC/DEC (HL) and INC/DEC (IX+d)/(IY+d) (see IncDecMemoryTable). The
 * underlying flag math is already hand-verified by
 * Test_IncDecRegister; this file's job is the new thing each tier adds -
 * reading/writing the right memory address and advancing PC correctly. */
public class Test_IncDecMemory {
	private Z80Machine machine;
	private Z80CPU cpu;

	@BeforeEach
	public void setup() {
		machine = new Z80Machine();
		Z80InstructionDecoder decoder = new Z80InstructionDecoder();
		IncDecMemoryTable.populate(decoder);
		cpu = new Z80CPU(machine.memoryBus(), machine.portBus(), decoder);
	}

	private void loadBytes(int... bytes) throws Exception {
		for (int i = 0; i < bytes.length; i++) {
			machine.memoryBus().write(0x1000 + i, (byte) bytes[i]);
		}
		cpu.pc = 0x1000;
	}

	@Test
	public void incHlIncrementsTheByteAtHlAndComputesFlags() throws Exception {
		cpu.hl.set16(0x3000);
		machine.memoryBus().write(0x3000, (byte) 0x7F);
		loadBytes(0x34); // INC (HL)

		cpu.step();

		assertEquals(0x80, machine.memoryBus().read(0x3000) & 0xff);
		assertEquals(Flags.S | Flags.H | Flags.PV, cpu.af.lo());
		assertEquals(0x1001, cpu.pc);
	}

	@Test
	public void decHlDecrementsTheByteAtHlAndComputesFlags() throws Exception {
		cpu.hl.set16(0x3100);
		machine.memoryBus().write(0x3100, (byte) 0x80);
		loadBytes(0x35); // DEC (HL)

		cpu.step();

		assertEquals(0x7F, machine.memoryBus().read(0x3100) & 0xff);
		assertEquals(Flags.H | Flags.PV | Flags.N, cpu.af.lo());
		assertEquals(0x1001, cpu.pc);
	}

	@Test
	public void incIxPlusDIncrementsTheRightByte() throws Exception {
		cpu.ix.set16(0x4000);
		machine.memoryBus().write(0x4005, (byte) 0x0F);
		loadBytes(0xDD, 0x34, 0x05); // INC (IX+5)

		cpu.step();

		assertEquals(0x10, machine.memoryBus().read(0x4005) & 0xff);
		assertEquals(Flags.H, cpu.af.lo());
		assertEquals(0x1003, cpu.pc, "prefix + opcode + displacement = 3 bytes consumed");
	}

	@Test
	public void decIyPlusDDecrementsTheRightByte() throws Exception {
		cpu.iy.set16(0x4100);
		machine.memoryBus().write(0x4105, (byte) 0x01);
		loadBytes(0xFD, 0x35, 0x05); // DEC (IY+5)

		cpu.step();

		assertEquals(0x00, machine.memoryBus().read(0x4105) & 0xff);
		assertEquals(Flags.Z | Flags.N, cpu.af.lo());
		assertEquals(0x1003, cpu.pc);
	}

	@Test
	public void negativeDisplacementWrapsTheAddressBackward() throws Exception {
		cpu.ix.set16(0x4000);
		machine.memoryBus().write(0x3FFF, (byte) 0x00); // IX + (-1)
		loadBytes(0xDD, 0x34, 0xFF); // INC (IX-1)

		cpu.step();

		assertEquals(0x01, machine.memoryBus().read(0x3FFF) & 0xff, "displacement must sign-extend, not be treated as +255");
	}
}
