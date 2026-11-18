package com.mj.z80.instructions;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.mj.z80.Flags;
import com.mj.z80.IndexRegister;
import com.mj.z80.RegisterPair;
import com.mj.z80.Z80CPU;
import com.mj.z80.Z80InstructionDecoder;
import com.mj.z80.Z80Machine;

/** ADD A,(IX+d) / ... / CP (IY+d) (see AluIndexedTable). Same "new
 * addressing, already-proven math" split as Test_AluImmediate/
 * Test_AluMemory, plus one dedicated check that a negative displacement
 * still sign-extends correctly in this context (the underlying mechanism
 * is shared with LoadFromIndexed, already proven there, but cheap enough
 * to confirm again here rather than assume). */
public class Test_AluIndexed {
	private Z80Machine machine;
	private Z80CPU cpu;

	@BeforeEach
	public void setup() {
		machine = new Z80Machine();
		Z80InstructionDecoder decoder = new Z80InstructionDecoder();
		AluIndexedTable.populate(decoder);
		cpu = new Z80CPU(machine.memoryBus(), machine.portBus(), decoder);
	}

	private int prefixOf(IndexRegister index) {
		return index == IndexRegister.IX ? 0xDD : 0xFD;
	}

	private RegisterPair registerOf(IndexRegister index) {
		return index == IndexRegister.IX ? cpu.ix : cpu.iy;
	}

	private void loadBytes(int... bytes) throws Exception {
		for (int i = 0; i < bytes.length; i++) {
			machine.memoryBus().write(0x1000 + i, (byte) bytes[i]);
		}
		cpu.pc = 0x1000;
	}

	@Test
	public void addAIxPlusDReadsTheByteAndComputesFlags() throws Exception {
		cpu.af.setHi(0x7F);
		cpu.ix.set16(0x4000);
		machine.memoryBus().write(0x4005, (byte) 0x01);
		loadBytes(0xDD, 0x86, 0x05); // ADD A,(IX+5)

		cpu.step();

		assertEquals(0x80, cpu.af.hi());
		assertEquals(Flags.S | Flags.H | Flags.PV, cpu.af.lo());
		assertEquals(0x1003, cpu.pc, "prefix + opcode + displacement = 3 bytes consumed");
	}

	@Test
	public void cpIyPlusDNeverWritesBackToA() throws Exception {
		cpu.af.setHi(0x05);
		cpu.iy.set16(0x4100);
		machine.memoryBus().write(0x4105, (byte) 0x05);
		loadBytes(0xFD, 0xBE, 0x05); // CP (IY+5)

		cpu.step();

		assertEquals(0x05, cpu.af.hi());
		assertEquals(Flags.Z | Flags.N, cpu.af.lo());
	}

	@Test
	public void negativeDisplacementWrapsTheAddressBackward() throws Exception {
		cpu.af.setHi(0x00);
		cpu.ix.set16(0x4000);
		machine.memoryBus().write(0x3FFF, (byte) 0x42); // IX + (-1)
		loadBytes(0xDD, 0x86, 0xFF); // ADD A,(IX-1)

		cpu.step();

		assertEquals(0x42, cpu.af.hi(), "displacement must sign-extend, not be treated as +255");
	}

	@Test
	public void everyOperationDispatchesToItsOwnIndexedOpcodeForBothIxAndIy() throws Exception {
		for (IndexRegister index : IndexRegister.values()) {
			for (AluOperation op : AluOperation.values()) {
				setup();
				cpu.af.setHi(0x10);
				registerOf(index).set16(0x4200);
				machine.memoryBus().write(0x4205, (byte) 0x03);
				int opcode = 0x80 | (op.code() << 3) | 0x06;
				loadBytes(prefixOf(index), opcode, 0x05);

				cpu.step();

				Z80CPU reference = new Z80CPU(machine.memoryBus(), machine.portBus(), null);
				reference.af.setHi(0x10);
				op.apply(reference, 0x03);

				String label = index + " opcode $" + Integer.toHexString(opcode) + " (" + op + ")";
				assertEquals(reference.af.hi(), cpu.af.hi(), label + " - A");
				assertEquals(reference.af.lo(), cpu.af.lo(), label + " - flags");
				assertEquals(0x1003, cpu.pc, label + " - PC");
			}
		}
	}
}
