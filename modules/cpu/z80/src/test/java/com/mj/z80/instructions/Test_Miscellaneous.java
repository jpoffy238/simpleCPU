package com.mj.z80.instructions;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.mj.z80.Flags;
import com.mj.z80.Z80CPU;
import com.mj.z80.Z80InstructionDecoder;
import com.mj.z80.Z80Machine;

/** EX DE,HL / EX AF,AF' / EXX / EX (SP),HL/IX/IY, DAA, CPL, unprefixed
 * ADD HL,rr, and RLD/RRD (see MiscellaneousTable). */
public class Test_Miscellaneous {
	private Z80Machine machine;
	private Z80CPU cpu;

	@BeforeEach
	public void setup() {
		machine = new Z80Machine();
		Z80InstructionDecoder decoder = new Z80InstructionDecoder();
		MiscellaneousTable.populate(decoder);
		cpu = new Z80CPU(machine.memoryBus(), machine.portBus(), decoder);
	}

	private void runBase(int opcode) throws Exception {
		machine.memoryBus().write(0x1000, (byte) opcode);
		cpu.pc = 0x1000;
		cpu.step();
	}

	private void runED(int edOpcode) throws Exception {
		machine.memoryBus().write(0x1000, (byte) 0xED);
		machine.memoryBus().write(0x1001, (byte) edOpcode);
		cpu.pc = 0x1000;
		cpu.step();
	}

	private void runDD(int opcode) throws Exception {
		machine.memoryBus().write(0x1000, (byte) 0xDD);
		machine.memoryBus().write(0x1001, (byte) opcode);
		cpu.pc = 0x1000;
		cpu.step();
	}

	private void runFD(int opcode) throws Exception {
		machine.memoryBus().write(0x1000, (byte) 0xFD);
		machine.memoryBus().write(0x1001, (byte) opcode);
		cpu.pc = 0x1000;
		cpu.step();
	}

	// ---- EX DE,HL / EX AF,AF' / EXX ----

	@Test
	public void exDeHlSwapsDeAndHl() throws Exception {
		cpu.de.set16(0x1234);
		cpu.hl.set16(0x5678);

		runBase(0xEB);

		assertEquals(0x5678, cpu.de.get16());
		assertEquals(0x1234, cpu.hl.get16());
	}

	@Test
	public void exAfShadowSwapsAfWithTheShadowSet() throws Exception {
		cpu.af.set16(0x1234);
		cpu.af2.set16(0x5678);

		runBase(0x08);

		assertEquals(0x5678, cpu.af.get16());
		assertEquals(0x1234, cpu.af2.get16());
	}

	@Test
	public void exxSwapsBcDeHlWithTheirShadowsButNotAf() throws Exception {
		cpu.bc.set16(0x1111);
		cpu.de.set16(0x2222);
		cpu.hl.set16(0x3333);
		cpu.bc2.set16(0xAAAA);
		cpu.de2.set16(0xBBBB);
		cpu.hl2.set16(0xCCCC);
		cpu.af.set16(0x9999);
		cpu.af2.set16(0x8888);

		runBase(0xD9);

		assertEquals(0xAAAA, cpu.bc.get16());
		assertEquals(0xBBBB, cpu.de.get16());
		assertEquals(0xCCCC, cpu.hl.get16());
		assertEquals(0x1111, cpu.bc2.get16());
		assertEquals(0x2222, cpu.de2.get16());
		assertEquals(0x3333, cpu.hl2.get16());
		assertEquals(0x9999, cpu.af.get16(), "EXX must not touch AF");
		assertEquals(0x8888, cpu.af2.get16(), "EXX must not touch AF'");
	}

	// ---- EX (SP),HL / IX / IY ----

	@Test
	public void exStackTopHlSwapsHlWithTheWordOnTopOfTheStack() throws Exception {
		cpu.sp = 0x3000;
		machine.memoryBus().write(0x3000, (byte) 0x34);
		machine.memoryBus().write(0x3001, (byte) 0x12); // stack top = 0x1234
		cpu.hl.set16(0x5678);

		runBase(0xE3);

		assertEquals(0x1234, cpu.hl.get16());
		assertEquals(0x78, machine.memoryBus().read(0x3000) & 0xff);
		assertEquals(0x56, machine.memoryBus().read(0x3001) & 0xff);
		assertEquals(0x3000, cpu.sp, "SP itself must not move");
	}

	@Test
	public void exStackTopIxSwapsIxWithTheWordOnTopOfTheStack() throws Exception {
		cpu.sp = 0x3000;
		machine.memoryBus().write(0x3000, (byte) 0x34);
		machine.memoryBus().write(0x3001, (byte) 0x12);
		cpu.ix.set16(0x5678);

		runDD(0xE3);

		assertEquals(0x1234, cpu.ix.get16());
		assertEquals(0x78, machine.memoryBus().read(0x3000) & 0xff);
		assertEquals(0x56, machine.memoryBus().read(0x3001) & 0xff);
	}

	@Test
	public void exStackTopIySwapsIyWithTheWordOnTopOfTheStack() throws Exception {
		cpu.sp = 0x3000;
		machine.memoryBus().write(0x3000, (byte) 0x34);
		machine.memoryBus().write(0x3001, (byte) 0x12);
		cpu.iy.set16(0x5678);

		runFD(0xE3);

		assertEquals(0x1234, cpu.iy.get16());
		assertEquals(0x78, machine.memoryBus().read(0x3000) & 0xff);
		assertEquals(0x56, machine.memoryBus().read(0x3001) & 0xff);
	}

	// ---- DAA ----

	@Test
	public void daaCorrectsALowNibbleOverflowAfterAnAdd() throws Exception {
		cpu.af.setHi(0x0A); // as if 0x09 + 0x01 had just been ADDed
		cpu.af.setLo(0); // H=0, N=0, C=0 - the ADD didn't set them

		runBase(0x27);

		assertEquals(0x10, cpu.af.hi());
		assertEquals(Flags.H, cpu.af.lo());
	}

	@Test
	public void daaCorrectsAHighNibbleOverflowAndCarryAfterAnAdd() throws Exception {
		cpu.af.setHi(0x20); // as if 0x90 + 0x90 had just been ADDed (wrapped to 0x20)
		cpu.af.setLo(Flags.C); // the ADD's own overflow past 0xFF

		runBase(0x27);

		assertEquals(0x80, cpu.af.hi());
		assertEquals(Flags.S | Flags.C, cpu.af.lo());
	}

	@Test
	public void daaCorrectsAHalfBorrowAfterASubtract() throws Exception {
		cpu.af.setHi(0x37); // as if 0x40 - 0x09 had just been SUBtracted
		cpu.af.setLo(Flags.H | Flags.N); // the SUB's own half-borrow, N marking it a subtract

		runBase(0x27);

		assertEquals(0x31, cpu.af.hi());
		assertEquals(Flags.N, cpu.af.lo());
	}

	// ---- CPL ----

	@Test
	public void cplFlipsEveryBitOfAAndSetsHAndNWhilePreservingEverythingElse() throws Exception {
		cpu.af.setHi(0x3C);
		cpu.af.setLo(Flags.S | Flags.Z | Flags.PV | Flags.C); // sentinel - all preserved

		runBase(0x2F);

		assertEquals(0xC3, cpu.af.hi());
		assertEquals(Flags.S | Flags.Z | Flags.PV | Flags.C | Flags.H | Flags.N, cpu.af.lo());
	}

	// ---- ADD HL,rr (unprefixed) ----

	@Test
	public void addHlBcAddsWithoutHalfCarryOrCarry() throws Exception {
		cpu.hl.set16(0x1234);
		cpu.bc.set16(0x1111);
		cpu.af.setLo(Flags.S | Flags.Z | Flags.PV); // sentinel - unaffected by this instruction

		runBase(0x09); // ADD HL,BC

		assertEquals(0x2345, cpu.hl.get16());
		assertEquals(Flags.S | Flags.Z | Flags.PV, cpu.af.lo());
	}

	@Test
	public void addHlSetsHalfCarryAtTheNibbleBoundary() throws Exception {
		cpu.hl.set16(0x0FFF);
		cpu.de.set16(0x0001);

		runBase(0x19); // ADD HL,DE

		assertEquals(0x1000, cpu.hl.get16());
		assertEquals(Flags.H, cpu.af.lo());
	}

	@Test
	public void addHlSetsCarryOnOverflowPastSixteenBits() throws Exception {
		cpu.hl.set16(0xFFFF);
		cpu.de.set16(0x0001);

		runBase(0x19); // ADD HL,DE

		assertEquals(0x0000, cpu.hl.get16());
		assertEquals(Flags.H | Flags.C, cpu.af.lo());
	}

	@Test
	public void addHlHlDoublesHl() throws Exception {
		cpu.hl.set16(0x1111);

		runBase(0x29); // ADD HL,HL

		assertEquals(0x2222, cpu.hl.get16());
	}

	@Test
	public void addHlSpWorks() throws Exception {
		cpu.hl.set16(0x1000);
		cpu.sp = 0x0234;

		runBase(0x39); // ADD HL,SP

		assertEquals(0x1234, cpu.hl.get16());
	}

	// ---- RLD / RRD ----

	@Test
	public void rldRotatesTheLowMemoryNibbleAndAHighNibbleThroughMemoryAndA() throws Exception {
		cpu.hl.set16(0x3000);
		machine.memoryBus().write(0x3000, (byte) 0x12);
		cpu.af.setHi(0x3F);

		runED(0x6F); // RLD

		assertEquals(0x2F, machine.memoryBus().read(0x3000) & 0xff);
		assertEquals(0x31, cpu.af.hi());
	}

	@Test
	public void rrdRotatesInTheOppositeDirectionFromRld() throws Exception {
		cpu.hl.set16(0x3000);
		machine.memoryBus().write(0x3000, (byte) 0x12);
		cpu.af.setHi(0x3F);

		runED(0x67); // RRD

		assertEquals(0xF1, machine.memoryBus().read(0x3000) & 0xff);
		assertEquals(0x32, cpu.af.hi());
	}

	@Test
	public void rldSetsZeroAndParityFlagsWhenTheNewAIsZero() throws Exception {
		cpu.hl.set16(0x3000);
		machine.memoryBus().write(0x3000, (byte) 0x05);
		cpu.af.setHi(0x00);
		cpu.af.setLo(Flags.C); // sentinel - C preserved

		runED(0x6F); // RLD

		assertEquals(0x00, cpu.af.hi());
		assertEquals(Flags.C | Flags.Z | Flags.PV, cpu.af.lo());
	}
}
