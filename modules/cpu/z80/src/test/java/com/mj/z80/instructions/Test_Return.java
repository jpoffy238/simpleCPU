package com.mj.z80.instructions;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.mj.z80.Flags;
import com.mj.z80.Z80CPU;
import com.mj.z80.Z80InstructionDecoder;
import com.mj.z80.Z80Machine;

/** RET / RET cc (see ReturnTable), plus a CALL-then-RET round trip tying
 * CallTable and ReturnTable together. */
public class Test_Return {
	private Z80Machine machine;
	private Z80CPU cpu;
	private Z80InstructionDecoder decoder;

	@BeforeEach
	public void setup() {
		machine = new Z80Machine();
		decoder = new Z80InstructionDecoder();
		ReturnTable.populate(decoder);
		cpu = new Z80CPU(machine.memoryBus(), machine.portBus(), decoder);
	}

	private void loadBytes(int... bytes) throws Exception {
		for (int i = 0; i < bytes.length; i++) {
			machine.memoryBus().write(0x1000 + i, (byte) bytes[i]);
		}
		cpu.pc = 0x1000;
	}

	@Test
	public void retPopsTheReturnAddressBackIntoPc() throws Exception {
		cpu.sp = 0x8FFE;
		machine.memoryBus().write(0x8FFE, (byte) 0x03);
		machine.memoryBus().write(0x8FFF, (byte) 0x10);
		loadBytes(0xC9); // RET

		cpu.step();

		assertEquals(0x1003, cpu.pc);
		assertEquals(0x9000, cpu.sp);
	}

	@Test
	public void retZTakenPopsAndJumps() throws Exception {
		cpu.sp = 0x8FFE;
		machine.memoryBus().write(0x8FFE, (byte) 0x03);
		machine.memoryBus().write(0x8FFF, (byte) 0x10);
		cpu.af.setLo(Flags.Z);
		loadBytes(0xC8); // RET Z

		cpu.step();

		assertEquals(0x1003, cpu.pc);
		assertEquals(0x9000, cpu.sp);
	}

	@Test
	public void retZNotTakenLeavesTheStackCompletelyUntouched() throws Exception {
		cpu.sp = 0x8FFE;
		cpu.af.setLo(0x00);
		loadBytes(0xC8); // RET Z

		cpu.step();

		assertEquals(0x1001, cpu.pc, "just past the 1-byte RET cc itself");
		assertEquals(0x8FFE, cpu.sp, "a failed RET cc must not touch the stack at all");
	}

	@Test
	public void callThenRetReturnsToRightAfterTheOriginalCall() throws Exception {
		CallTable.populate(decoder); // same decoder, both tables coexist
		cpu.sp = 0x9000;
		machine.memoryBus().write(0x1000, (byte) 0xCD); // CALL $2000
		machine.memoryBus().write(0x1001, (byte) 0x00);
		machine.memoryBus().write(0x1002, (byte) 0x20);
		machine.memoryBus().write(0x2000, (byte) 0xC9); // RET, at the call target
		cpu.pc = 0x1000;

		cpu.step(); // CALL $2000
		assertEquals(0x2000, cpu.pc);

		cpu.step(); // RET
		assertEquals(0x1003, cpu.pc, "right after the original 3-byte CALL");
		assertEquals(0x9000, cpu.sp, "SP back to where it started");
	}
}
