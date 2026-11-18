package com.mj.z80.instructions;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.mj.z80.Z80CPU;
import com.mj.z80.Z80InstructionDecoder;
import com.mj.z80.Z80Machine;

/** RST p (see RestartTable). */
public class Test_Restart {
	private Z80Machine machine;
	private Z80CPU cpu;

	@BeforeEach
	public void setup() {
		machine = new Z80Machine();
		Z80InstructionDecoder decoder = new Z80InstructionDecoder();
		RestartTable.populate(decoder);
		cpu = new Z80CPU(machine.memoryBus(), machine.portBus(), decoder);
	}

	private void loadBytes(int... bytes) throws Exception {
		for (int i = 0; i < bytes.length; i++) {
			machine.memoryBus().write(0x1000 + i, (byte) bytes[i]);
		}
		cpu.pc = 0x1000;
	}

	@Test
	public void rst0JumpsToAddressZero() throws Exception {
		cpu.sp = 0x9000;
		loadBytes(0xC7); // RST 0

		cpu.step();

		assertEquals(0x0000, cpu.pc);
		assertEquals(0x8FFE, cpu.sp);
		assertEquals(0x01, machine.memoryBus().read(0x8FFE) & 0xff, "return address low byte - 0x1001, past the 1-byte RST");
		assertEquals(0x10, machine.memoryBus().read(0x8FFF) & 0xff, "return address high byte");
	}

	@Test
	public void everyRestartTargetIsPTimesEight() throws Exception {
		for (int p = 0; p < 8; p++) {
			setup();
			cpu.sp = 0x9000;
			loadBytes(0xC7 | (p << 3));

			cpu.step();

			assertEquals(p * 8, cpu.pc, "RST " + p);
			assertEquals(0x8FFE, cpu.sp);
		}
	}
}
