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

/** RLC/.../SRL (IX+d)/(IY+d) (see RotateShiftTable) - the DD-CB/FD-CB
 * forms. Unlike every other indexed instruction tested so far, the
 * displacement byte comes BEFORE the final opcode in the instruction
 * stream (prefix, $CB, displacement, opcode) and is read by the decoder
 * itself, not by this instruction's own execute() - so these tests are
 * also exercising Z80CPU.pendingDisplacement end to end for the first
 * time since the fetch/decode loop was built. */
public class Test_RotateShiftIndexed {
	private Z80Machine machine;
	private Z80CPU cpu;

	@BeforeEach
	public void setup() {
		machine = new Z80Machine();
		Z80InstructionDecoder decoder = new Z80InstructionDecoder();
		RotateShiftTable.populate(decoder);
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
	public void rlcIxPlusDRotatesTheByteAtTheDisplacedAddress() throws Exception {
		cpu.ix.set16(0x4000);
		machine.memoryBus().write(0x4005, (byte) 0x80);
		loadBytes(0xDD, 0xCB, 0x05, 0x06); // RLC (IX+5)

		cpu.step();

		assertEquals(0x01, machine.memoryBus().read(0x4005) & 0xff);
		assertEquals(Flags.C, cpu.af.lo());
		assertEquals(0x1004, cpu.pc, "prefix + $CB + displacement + opcode = 4 bytes consumed");
	}

	@Test
	public void negativeDisplacementWrapsTheAddressBackward() throws Exception {
		cpu.ix.set16(0x4000);
		machine.memoryBus().write(0x3FFF, (byte) 0x80); // IX + (-1)
		loadBytes(0xDD, 0xCB, 0xFF, 0x06); // RLC (IX-1)

		cpu.step();

		assertEquals(0x01, machine.memoryBus().read(0x3FFF) & 0xff, "displacement must sign-extend, not be treated as +255");
	}

	@Test
	public void everyOperationDispatchesToItsOwnIndexedOpcodeForBothIxAndIy() throws Exception {
		for (IndexRegister index : IndexRegister.values()) {
			for (RotateShiftOperation op : RotateShiftOperation.values()) {
				setup();
				registerOf(index).set16(0x4200);
				machine.memoryBus().write(0x4205, (byte) 0x55);
				int opcode = (op.code() << 3) | 0x06;
				loadBytes(prefixOf(index), 0xCB, 0x05, opcode);

				cpu.step();

				Z80CPU reference = new Z80CPU(machine.memoryBus(), machine.portBus(), null);
				int expected = op.apply(reference, 0x55);

				String label = index + " opcode $CB $" + Integer.toHexString(opcode) + " (" + op + ")";
				assertEquals(expected, machine.memoryBus().read(0x4205) & 0xff, label + " - result");
				assertEquals(reference.af.lo(), cpu.af.lo(), label + " - flags");
				assertEquals(0x1004, cpu.pc, label + " - PC");
			}
		}
	}
}
