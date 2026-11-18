package com.mj.z80.instructions;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.mj.z80.Flags;
import com.mj.z80.Z80CPU;
import com.mj.z80.Z80InstructionDecoder;
import com.mj.z80.Z80Machine;

/** CALL nn / CALL cc,nn (see CallTable). */
public class Test_Call {
	private Z80Machine machine;
	private Z80CPU cpu;

	@BeforeEach
	public void setup() {
		machine = new Z80Machine();
		Z80InstructionDecoder decoder = new Z80InstructionDecoder();
		CallTable.populate(decoder);
		cpu = new Z80CPU(machine.memoryBus(), machine.portBus(), decoder);
	}

	private void loadBytes(int... bytes) throws Exception {
		for (int i = 0; i < bytes.length; i++) {
			machine.memoryBus().write(0x1000 + i, (byte) bytes[i]);
		}
		cpu.pc = 0x1000;
	}

	@Test
	public void callNnPushesTheRealReturnAddressAndJumps() throws Exception {
		cpu.sp = 0x9000;
		loadBytes(0xCD, 0x34, 0x12); // CALL $1234

		cpu.step();

		assertEquals(0x1234, cpu.pc);
		assertEquals(0x8FFE, cpu.sp);
		assertEquals(0x03, machine.memoryBus().read(0x8FFE) & 0xff, "low byte of the return address, 0x1003");
		assertEquals(0x10, machine.memoryBus().read(0x8FFF) & 0xff, "high byte - past the 3-byte CALL, not pointing at it");
	}

	@Test
	public void callZTakenPushesAndJumps() throws Exception {
		cpu.sp = 0x9000;
		cpu.af.setLo(Flags.Z);
		loadBytes(0xCC, 0x34, 0x12); // CALL Z,$1234

		cpu.step();

		assertEquals(0x1234, cpu.pc);
		assertEquals(0x8FFE, cpu.sp);
	}

	@Test
	public void callZNotTakenLeavesTheStackCompletelyUntouched() throws Exception {
		cpu.sp = 0x9000;
		cpu.af.setLo(0x00);
		loadBytes(0xCC, 0x34, 0x12); // CALL Z,$1234

		cpu.step();

		assertEquals(0x1003, cpu.pc, "address bytes still consumed, just no jump");
		assertEquals(0x9000, cpu.sp, "a failed CALL cc must not push anything at all");
	}
}
