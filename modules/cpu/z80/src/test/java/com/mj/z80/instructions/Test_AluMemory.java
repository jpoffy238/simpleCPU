package com.mj.z80.instructions;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.mj.z80.Flags;
import com.mj.z80.Z80CPU;
import com.mj.z80.Z80InstructionDecoder;
import com.mj.z80.Z80Machine;

/** ADD A,(HL) / ... / CP (HL) (see AluMemoryTable). Same "new addressing,
 * already-proven math" split as Test_AluImmediate. */
public class Test_AluMemory {
	private Z80Machine machine;
	private Z80CPU cpu;

	@BeforeEach
	public void setup() {
		machine = new Z80Machine();
		Z80InstructionDecoder decoder = new Z80InstructionDecoder();
		AluMemoryTable.populate(decoder);
		cpu = new Z80CPU(machine.memoryBus(), machine.portBus(), decoder);
	}

	private void loadBytes(int... bytes) throws Exception {
		for (int i = 0; i < bytes.length; i++) {
			machine.memoryBus().write(0x1000 + i, (byte) bytes[i]);
		}
		cpu.pc = 0x1000;
	}

	@Test
	public void addAHlReadsTheByteAtHlAndComputesFlags() throws Exception {
		cpu.af.setHi(0x7F);
		cpu.hl.set16(0x3000);
		machine.memoryBus().write(0x3000, (byte) 0x01);
		loadBytes(0x86); // ADD A,(HL)

		cpu.step();

		assertEquals(0x80, cpu.af.hi());
		assertEquals(Flags.S | Flags.H | Flags.PV, cpu.af.lo());
		assertEquals(0x1001, cpu.pc, "one byte consumed - the operand lives at [HL], not in the instruction stream");
	}

	@Test
	public void cpHlNeverWritesBackToA() throws Exception {
		cpu.af.setHi(0x05);
		cpu.hl.set16(0x3100);
		machine.memoryBus().write(0x3100, (byte) 0x05);
		loadBytes(0xBE); // CP (HL)

		cpu.step();

		assertEquals(0x05, cpu.af.hi());
		assertEquals(Flags.Z | Flags.N, cpu.af.lo());
	}

	@Test
	public void everyOperationDispatchesToItsOwnHlOpcode() throws Exception {
		for (AluOperation op : AluOperation.values()) {
			setup();
			cpu.af.setHi(0x10);
			cpu.hl.set16(0x3200);
			machine.memoryBus().write(0x3200, (byte) 0x03);
			int opcode = 0x80 | (op.code() << 3) | 0x06;
			loadBytes(opcode);

			cpu.step();

			Z80CPU reference = new Z80CPU(machine.memoryBus(), machine.portBus(), null);
			reference.af.setHi(0x10);
			op.apply(reference, 0x03);

			String label = "opcode $" + Integer.toHexString(opcode) + " (" + op + ")";
			assertEquals(reference.af.hi(), cpu.af.hi(), label + " - A");
			assertEquals(reference.af.lo(), cpu.af.lo(), label + " - flags");
			assertEquals(0x1001, cpu.pc, label + " - PC");
		}
	}
}
