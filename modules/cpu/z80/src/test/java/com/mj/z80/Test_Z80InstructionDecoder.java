package com.mj.z80;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.mj.exceptions.DeviceUnavailable;
import com.mj.exceptions.illegalAddressException;
import com.mj.exceptions.illegalOpCodeException;
import com.mj.z80.instructions.NOP;

/** Proves the prefix-cascade mechanism itself (Z80InstructionDecoder) works
 * for every one of the Z80's opcode spaces, independent of any real
 * instruction's behavior - that's the next phase, one family at a time.
 * Each test loads a tiny hand-built byte sequence into RAM, points PC at
 * it, steps the CPU once, and checks which table slot actually ran and how
 * far PC moved. */
public class Test_Z80InstructionDecoder {
	private Z80Machine machine;
	private Z80InstructionDecoder decoder;
	private Z80CPU cpu;

	@BeforeEach
	public void setup() {
		machine = new Z80Machine();
		decoder = new Z80InstructionDecoder();
		cpu = new Z80CPU(machine.memoryBus(), machine.portBus(), decoder);
	}

	private void load(int address, int... bytes) throws Exception {
		for (int i = 0; i < bytes.length; i++) {
			machine.memoryBus().write(address + i, (byte) bytes[i]);
		}
		cpu.pc = address;
	}

	/** A test-only instruction recording that it ran and how many extra
	 * bytes (beyond whatever the decoder itself already consumed to
	 * identify it) it read for itself - exactly what a real DD/FD
	 * instruction reading its own trailing displacement/operand would do
	 * via cpu.fetchByte(), per Z80CPU's own convention. */
	private static final class Probe implements Z80Instruction {
		boolean ran;
		int selfReadByte = -1;

		@Override
		public void execute(Z80CPU cpu) throws illegalAddressException, DeviceUnavailable {
			ran = true;
			selfReadByte = cpu.fetchByte() & 0xff;
		}
	}

	@Test
	public void unprefixedOpcodeDispatchesAndAdvancesPcByOne() throws Exception {
		decoder.setBase(0x00, NOP.INSTANCE);
		load(0x1000, 0x00);

		cpu.step();

		assertEquals(0x1001, cpu.pc);
	}

	@Test
	public void cbPrefixedOpcodeConsumesTwoBytesBeforeDispatching() throws Exception {
		Probe probe = new Probe();
		decoder.setCB(0x37, probe);
		load(0x2000, 0xCB, 0x37, 0x99);

		cpu.step();

		assertEquals(true, probe.ran);
		assertEquals(0x99, probe.selfReadByte, "the instruction's own fetchByte() should see the byte right after $CB <opcode>");
		assertEquals(0x2003, cpu.pc);
	}

	@Test
	public void edPrefixedOpcodeConsumesTwoBytesBeforeDispatching() throws Exception {
		Probe probe = new Probe();
		decoder.setED(0x44, probe);
		load(0x2100, 0xED, 0x44, 0x77);

		cpu.step();

		assertEquals(true, probe.ran);
		assertEquals(0x77, probe.selfReadByte);
		assertEquals(0x2103, cpu.pc);
	}

	@Test
	public void ddPrefixedNonCbOpcodeLetsTheInstructionReadItsOwnTrailingByte() throws Exception {
		// e.g. a real ADD A,(IX+d)-shaped instruction: the decoder only
		// needs $DD + the opcode byte to identify it; the displacement is
		// the instruction's own problem, read exactly like any ordinary
		// operand byte (Z80CPU's fetchByte() convention), not pre-consumed
		// by the decoder the way DD-CB's displacement is.
		Probe probe = new Probe();
		decoder.setDD(0x86, probe);
		load(0x2200, 0xDD, 0x86, 0x05);

		cpu.step();

		assertEquals(true, probe.ran);
		assertEquals(0x05, probe.selfReadByte);
		assertEquals(0x2203, cpu.pc);
	}

	@Test
	public void fdPrefixedNonCbOpcodeMirrorsDd() throws Exception {
		Probe probe = new Probe();
		decoder.setFD(0x86, probe);
		load(0x2300, 0xFD, 0x86, 0x06);

		cpu.step();

		assertEquals(true, probe.ran);
		assertEquals(0x06, probe.selfReadByte);
		assertEquals(0x2303, cpu.pc);
	}

	@Test
	public void ddCbFormReadsTheDisplacementBeforeTheFinalOpcodeAndStashesItOnTheCpu() throws Exception {
		Probe probe = new Probe();
		decoder.setDDCB(0x06, probe); // e.g. RLC (IX+d)'s final opcode byte
		load(0x2400, 0xDD, 0xCB, 0x2A, 0x06, 0x11);

		cpu.step();

		assertEquals((byte) 0x2A, cpu.pendingDisplacement, "decoder must stash the displacement before the instruction runs");
		assertEquals(true, probe.ran);
		assertEquals(0x11, probe.selfReadByte, "PC should be positioned right after the final opcode, not the displacement");
		assertEquals(0x2405, cpu.pc);
	}

	@Test
	public void fdCbFormMirrorsDdCb() throws Exception {
		Probe probe = new Probe();
		decoder.setFDCB(0x06, probe);
		load(0x2500, 0xFD, 0xCB, 0xF0, 0x06, 0x22);

		cpu.step();

		assertEquals((byte) 0xF0, cpu.pendingDisplacement);
		assertEquals(true, probe.ran);
		assertEquals(0x22, probe.selfReadByte);
		assertEquals(0x2505, cpu.pc);
	}

	@Test
	public void anUndefinedBaseOpcodeThrows() throws Exception {
		load(0x2600, 0x00); // $00 deliberately left unregistered in this test's decoder

		assertThrows(illegalOpCodeException.class, cpu::step);
	}

	@Test
	public void anUndefinedCbOpcodeThrows() throws Exception {
		load(0x2700, 0xCB, 0x00);

		assertThrows(illegalOpCodeException.class, cpu::step);
	}

	@Test
	public void anUndefinedDdCbOpcodeThrows() throws Exception {
		load(0x2800, 0xDD, 0xCB, 0x00, 0x00);

		assertThrows(illegalOpCodeException.class, cpu::step);
	}

	/** Proves out the "one parameterized builder generates both the DD and
	 * FD tables" design: the same instruction behavior, built once as a
	 * function of "which index register", produces two distinct
	 * Z80Instruction instances (one per call, each closing over its own
	 * RegisterPair) rather than needing Z80CPU to track "which register" as
	 * mutable per-call state the way pendingDisplacement does. */
	@Test
	public void theSameIndexedInstructionShapeWorksForBothIxAndIy() throws Exception {
		int opcode = 0x21; // e.g. LD IX,nn / LD IY,nn's real opcode
		decoder.setDD(opcode, loadIndexRegisterWithSixteen(cpu.ix));
		decoder.setFD(opcode, loadIndexRegisterWithSixteen(cpu.iy));

		load(0x2900, 0xDD, 0x21);
		cpu.step();
		assertEquals(16, cpu.ix.get16());
		assertEquals(0, cpu.iy.get16(), "FD's own call shouldn't have touched IX's sibling");

		load(0x2A00, 0xFD, 0x21);
		cpu.step();
		assertEquals(16, cpu.iy.get16());
	}

	private static Z80Instruction loadIndexRegisterWithSixteen(RegisterPair register) {
		return cpu -> register.set16(16);
	}
}
