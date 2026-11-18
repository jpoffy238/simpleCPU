package com.mj.z80.instructions;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.mj.z80.Register8;
import com.mj.z80.Z80CPU;
import com.mj.z80.Z80InstructionDecoder;
import com.mj.z80.Z80Machine;

/** RES b,r/(HL)/(IX+d)/(IY+d) and SET b,r/(HL)/(IX+d)/(IY+d) (see
 * BitTable). Both affect NO flag at all - a third "no flags" family
 * alongside 16-bit INC/DEC and PUSH/POP - pinned explicitly with a
 * flags sentinel throughout. */
public class Test_ModifyBit {
	private Z80Machine machine;
	private Z80CPU cpu;

	@BeforeEach
	public void setup() {
		machine = new Z80Machine();
		Z80InstructionDecoder decoder = new Z80InstructionDecoder();
		BitTable.populate(decoder);
		cpu = new Z80CPU(machine.memoryBus(), machine.portBus(), decoder);
	}

	private void run(int cbOpcode) throws Exception {
		machine.memoryBus().write(0x1000, (byte) 0xCB);
		machine.memoryBus().write(0x1001, (byte) cbOpcode);
		cpu.pc = 0x1000;
		cpu.step();
	}

	@Test
	public void resClearsOnlyTheTargetBitAndAffectsNoFlag() throws Exception {
		cpu.bc.setLo(0xFF);
		cpu.af.setLo(0x55);

		run(0x81); // RES 0,C

		assertEquals(0xFE, cpu.bc.lo());
		assertEquals(0x55, cpu.af.lo(), "RES must never affect flags");
		assertEquals(0x1002, cpu.pc);
	}

	@Test
	public void setSetsOnlyTheTargetBitAndAffectsNoFlag() throws Exception {
		cpu.bc.setLo(0x00);
		cpu.af.setLo(0x55);

		run(0xC1); // SET 0,C

		assertEquals(0x01, cpu.bc.lo());
		assertEquals(0x55, cpu.af.lo(), "SET must never affect flags");
	}

	@Test
	public void resHlClearsTheBitInMemoryWithoutTouchingFlags() throws Exception {
		cpu.hl.set16(0x3000);
		machine.memoryBus().write(0x3000, (byte) 0xFF);
		cpu.af.setLo(0x55);

		run(0x86); // RES 0,(HL)

		assertEquals(0xFE, machine.memoryBus().read(0x3000) & 0xff);
		assertEquals(0x55, cpu.af.lo());
	}

	@Test
	public void setIxPlusDSetsTheBitAtTheDisplacedAddress() throws Exception {
		cpu.ix.set16(0x4000);
		machine.memoryBus().write(0x4005, (byte) 0x00);
		machine.memoryBus().write(0x1000, (byte) 0xDD);
		machine.memoryBus().write(0x1001, (byte) 0xCB);
		machine.memoryBus().write(0x1002, (byte) 0x05);
		machine.memoryBus().write(0x1003, (byte) 0xC6); // SET 0,(IX+5)
		cpu.pc = 0x1000;

		cpu.step();

		assertEquals(0x01, machine.memoryBus().read(0x4005) & 0xff);
		assertEquals(0x1004, cpu.pc);
	}

	@Test
	public void everyBitAndRegisterResetsAndSetsCorrectly() throws Exception {
		for (Register8 r : Register8.values()) {
			for (int bit = 0; bit < 8; bit++) {
				setup();
				r.set(cpu, 0xFF);
				int resOpcode = 0x80 | (bit << 3) | r.code();
				run(resOpcode);
				assertEquals(0xFF & ~(1 << bit), r.get(cpu), "RES " + bit + "," + r);

				setup();
				r.set(cpu, 0x00);
				int setOpcode = 0xC0 | (bit << 3) | r.code();
				run(setOpcode);
				assertEquals(1 << bit, r.get(cpu), "SET " + bit + "," + r);
			}
		}
	}
}
