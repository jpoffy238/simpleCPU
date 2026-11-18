package com.mj.z80.instructions;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.mj.z80.Flags;
import com.mj.z80.Z80CPU;
import com.mj.z80.Z80InstructionDecoder;
import com.mj.z80.Z80Machine;

/** RLC/.../SRL (HL) (see RotateShiftTable). Same "new addressing,
 * already-proven math" split used throughout - Test_RotateShiftRegister
 * hand-verifies the operations themselves. */
public class Test_RotateShiftMemory {
	private Z80Machine machine;
	private Z80CPU cpu;

	@BeforeEach
	public void setup() {
		machine = new Z80Machine();
		Z80InstructionDecoder decoder = new Z80InstructionDecoder();
		RotateShiftTable.populate(decoder);
		cpu = new Z80CPU(machine.memoryBus(), machine.portBus(), decoder);
	}

	private void loadBytes(int... bytes) throws Exception {
		for (int i = 0; i < bytes.length; i++) {
			machine.memoryBus().write(0x1000 + i, (byte) bytes[i]);
		}
		cpu.pc = 0x1000;
	}

	@Test
	public void rlcHlRotatesTheByteAtHl() throws Exception {
		cpu.hl.set16(0x3000);
		machine.memoryBus().write(0x3000, (byte) 0x80);
		loadBytes(0xCB, 0x06); // RLC (HL)

		cpu.step();

		assertEquals(0x01, machine.memoryBus().read(0x3000) & 0xff);
		assertEquals(Flags.C, cpu.af.lo());
		assertEquals(0x1002, cpu.pc);
	}

	@Test
	public void everyOperationDispatchesToItsOwnHlOpcode() throws Exception {
		for (RotateShiftOperation op : RotateShiftOperation.values()) {
			setup();
			cpu.hl.set16(0x3200);
			machine.memoryBus().write(0x3200, (byte) 0x55);
			int opcode = (op.code() << 3) | 0x06;
			loadBytes(0xCB, opcode);

			cpu.step();

			Z80CPU reference = new Z80CPU(machine.memoryBus(), machine.portBus(), null);
			int expected = op.apply(reference, 0x55);

			String label = "opcode $CB $" + Integer.toHexString(opcode) + " (" + op + ")";
			assertEquals(expected, machine.memoryBus().read(0x3200) & 0xff, label + " - result");
			assertEquals(reference.af.lo(), cpu.af.lo(), label + " - flags");
			assertEquals(0x1002, cpu.pc, label + " - PC");
		}
	}
}
