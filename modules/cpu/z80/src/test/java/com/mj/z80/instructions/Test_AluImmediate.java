package com.mj.z80.instructions;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.mj.z80.Flags;
import com.mj.z80.Z80CPU;
import com.mj.z80.Z80InstructionDecoder;
import com.mj.z80.Z80Machine;

/** ADD A,n / ADC A,n / SUB n / SBC A,n / AND n / XOR n / OR n / CP n (see
 * AluImmediateTable). The underlying flag math is already hand-verified
 * by Test_AluRegister; this file's job is the new thing this tier adds -
 * reading the literal byte correctly and advancing PC by 2 - plus a
 * dispatch sweep confirming each opcode reaches the right AluOperation. */
public class Test_AluImmediate {
	private Z80Machine machine;
	private Z80CPU cpu;

	@BeforeEach
	public void setup() {
		machine = new Z80Machine();
		Z80InstructionDecoder decoder = new Z80InstructionDecoder();
		AluImmediateTable.populate(decoder);
		cpu = new Z80CPU(machine.memoryBus(), machine.portBus(), decoder);
	}

	private void loadBytes(int... bytes) throws Exception {
		for (int i = 0; i < bytes.length; i++) {
			machine.memoryBus().write(0x1000 + i, (byte) bytes[i]);
		}
		cpu.pc = 0x1000;
	}

	@Test
	public void addANReadsTheLiteralByteAndComputesFlags() throws Exception {
		cpu.af.setHi(0x7F);
		loadBytes(0xC6, 0x01); // ADD A,n

		cpu.step();

		assertEquals(0x80, cpu.af.hi());
		assertEquals(Flags.S | Flags.H | Flags.PV, cpu.af.lo());
		assertEquals(0x1002, cpu.pc, "opcode + immediate byte = 2 bytes consumed");
	}

	@Test
	public void cpNNeverWritesBackToA() throws Exception {
		cpu.af.setHi(0x05);
		loadBytes(0xFE, 0x05); // CP n

		cpu.step();

		assertEquals(0x05, cpu.af.hi(), "A must be unchanged - CP only ever sets flags");
		assertEquals(Flags.Z | Flags.N, cpu.af.lo());
	}

	/** For each operation, the decoder path (opcode + immediate byte,
	 * decoded and executed) must agree exactly with calling the same
	 * AluOperation directly on an identical starting state - proving the
	 * opcode formula reaches the right operation, without re-deriving its
	 * flag math a second time (Test_AluRegister already pins that). */
	@Test
	public void everyOperationDispatchesToItsOwnImmediateOpcode() throws Exception {
		for (AluOperation op : AluOperation.values()) {
			setup();
			cpu.af.setHi(0x10);
			int opcode = 0xC6 | (op.code() << 3);
			loadBytes(opcode, 0x03);

			cpu.step();

			Z80CPU reference = new Z80CPU(machine.memoryBus(), machine.portBus(), null);
			reference.af.setHi(0x10);
			op.apply(reference, 0x03);

			String label = "opcode $" + Integer.toHexString(opcode) + " (" + op + ")";
			assertEquals(reference.af.hi(), cpu.af.hi(), label + " - A");
			assertEquals(reference.af.lo(), cpu.af.lo(), label + " - flags");
			assertEquals(0x1002, cpu.pc, label + " - PC");
		}
	}
}
