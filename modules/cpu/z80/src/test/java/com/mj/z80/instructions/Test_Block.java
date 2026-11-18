package com.mj.z80.instructions;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.mj.z80.Flags;
import com.mj.z80.Z80CPU;
import com.mj.z80.Z80InstructionDecoder;
import com.mj.z80.Z80Machine;

/** LDI/LDD/LDIR/LDDR and CPI/CPD/CPIR/CPDR (see BlockTable). */
public class Test_Block {
	private Z80Machine machine;
	private Z80CPU cpu;

	@BeforeEach
	public void setup() {
		machine = new Z80Machine();
		Z80InstructionDecoder decoder = new Z80InstructionDecoder();
		BlockTable.populate(decoder);
		cpu = new Z80CPU(machine.memoryBus(), machine.portBus(), decoder);
	}

	private void run(int edOpcode) throws Exception {
		machine.memoryBus().write(0x1000, (byte) 0xED);
		machine.memoryBus().write(0x1001, (byte) edOpcode);
		cpu.pc = 0x1000;
		cpu.step();
	}

	// ---- LDI / LDD ----

	@Test
	public void ldiCopiesOneByteAndStepsHlAndDeForward() throws Exception {
		cpu.hl.set16(0x3000);
		cpu.de.set16(0x4000);
		cpu.bc.set16(0x0002);
		machine.memoryBus().write(0x3000, (byte) 0x55);
		cpu.af.setLo(Flags.S | Flags.Z | Flags.C); // sentinel - LDI must preserve S/Z/C untouched

		run(0xA0); // LDI

		assertEquals(0x55, machine.memoryBus().read(0x4000) & 0xff);
		assertEquals(0x3001, cpu.hl.get16());
		assertEquals(0x4001, cpu.de.get16());
		assertEquals(0x0001, cpu.bc.get16());
		assertEquals(Flags.S | Flags.Z | Flags.C | Flags.PV, cpu.af.lo(), "S/Z/C preserved; PV set since BC is still nonzero");
		assertEquals(0x1002, cpu.pc, "prefix + opcode = 2 bytes consumed");
	}

	@Test
	public void ldiClearsPvOnceBcReachesZero() throws Exception {
		cpu.hl.set16(0x3000);
		cpu.de.set16(0x4000);
		cpu.bc.set16(0x0001);
		machine.memoryBus().write(0x3000, (byte) 0x55);

		run(0xA0); // LDI

		assertEquals(0x0000, cpu.bc.get16());
		assertEquals(0, cpu.af.lo() & Flags.PV);
	}

	@Test
	public void lddStepsHlAndDeBackward() throws Exception {
		cpu.hl.set16(0x3000);
		cpu.de.set16(0x4000);
		cpu.bc.set16(0x0002);
		machine.memoryBus().write(0x3000, (byte) 0x66);

		run(0xA8); // LDD

		assertEquals(0x66, machine.memoryBus().read(0x4000) & 0xff);
		assertEquals(0x2FFF, cpu.hl.get16());
		assertEquals(0x3FFF, cpu.de.get16());
	}

	// ---- LDIR / LDDR ----

	@Test
	public void ldirCopiesTheWholeBlockAndStopsWithBcZero() throws Exception {
		cpu.hl.set16(0x3000);
		cpu.de.set16(0x4000);
		cpu.bc.set16(0x0003);
		machine.memoryBus().write(0x3000, (byte) 0x11);
		machine.memoryBus().write(0x3001, (byte) 0x22);
		machine.memoryBus().write(0x3002, (byte) 0x33);

		run(0xB0); // LDIR

		assertEquals(0x11, machine.memoryBus().read(0x4000) & 0xff);
		assertEquals(0x22, machine.memoryBus().read(0x4001) & 0xff);
		assertEquals(0x33, machine.memoryBus().read(0x4002) & 0xff);
		assertEquals(0x3003, cpu.hl.get16());
		assertEquals(0x4003, cpu.de.get16());
		assertEquals(0x0000, cpu.bc.get16());
		assertEquals(0, cpu.af.lo() & Flags.PV, "the loop only ever stops once BC is 0, so PV is always clear afterward");
		assertEquals(0x1002, cpu.pc, "still just prefix + opcode, even though the whole block was copied in one step()");
	}

	@Test
	public void lddrCopiesTheWholeBlockBackward() throws Exception {
		cpu.hl.set16(0x3002);
		cpu.de.set16(0x4002);
		cpu.bc.set16(0x0003);
		machine.memoryBus().write(0x3000, (byte) 0x11);
		machine.memoryBus().write(0x3001, (byte) 0x22);
		machine.memoryBus().write(0x3002, (byte) 0x33);

		run(0xB8); // LDDR

		assertEquals(0x11, machine.memoryBus().read(0x4000) & 0xff);
		assertEquals(0x22, machine.memoryBus().read(0x4001) & 0xff);
		assertEquals(0x33, machine.memoryBus().read(0x4002) & 0xff);
		assertEquals(0x2FFF, cpu.hl.get16());
		assertEquals(0x3FFF, cpu.de.get16());
	}

	@Test
	public void ldirWithBcZeroAtTheStartRunsAFullSixtyFiveThousandFiveHundredThirtySixPasses() throws Exception {
		cpu.hl.set16(0x0000);
		cpu.de.set16(0x8000);
		cpu.bc.set16(0x0000); // real hardware's own quirk: 0 decrements to 0xFFFF first, not "nothing to do"
		machine.memoryBus().write(0x0000, (byte) 0xAA);

		run(0xB0); // LDIR

		// Source (0x0000-0xFFFF) and destination (0x8000-0xFFFF then
		// 0x0000-0x7FFF) both span the entire 64K space on a full pass, so
		// they overlap - the destination sweep overwrites 0xFFFF partway
		// through, before the source sweep would otherwise reach it again,
		// same as real hardware's own overlap-sensitive LDIR. Only the
		// very first byte is guaranteed untouched by that overlap.
		assertEquals(0xAA, machine.memoryBus().read(0x8000) & 0xff, "first byte of the wrap, unaffected by the overlap");
		assertEquals(0x0000, cpu.hl.get16(), "HL wrapped exactly once back to its start");
		assertEquals(0x8000, cpu.de.get16());
		assertEquals(0x0000, cpu.bc.get16());
	}

	// ---- CPI / CPD ----

	@Test
	public void cpiFindsAMatchAndSetsZero() throws Exception {
		cpu.af.setHi(0x05);
		cpu.af.setLo(Flags.C); // sentinel - CPI must preserve C, unlike an ordinary CP
		cpu.hl.set16(0x3000);
		cpu.bc.set16(0x0002);
		machine.memoryBus().write(0x3000, (byte) 0x05);

		run(0xA1); // CPI

		assertEquals(0x05, cpu.af.hi(), "CPI never writes back to A, same as an ordinary CP");
		assertEquals(0x3001, cpu.hl.get16());
		assertEquals(0x0001, cpu.bc.get16());
		assertEquals(Flags.C | Flags.Z | Flags.PV | Flags.N, cpu.af.lo(), "C preserved; Z set on match; PV set since BC is still nonzero");
	}

	@Test
	public void cpiNoMatchClearsZero() throws Exception {
		cpu.af.setHi(0x05);
		cpu.hl.set16(0x3000);
		cpu.bc.set16(0x0002);
		machine.memoryBus().write(0x3000, (byte) 0x03);

		run(0xA1); // CPI

		assertEquals(Flags.PV | Flags.N, cpu.af.lo());
	}

	@Test
	public void cpdStepsHlBackward() throws Exception {
		cpu.af.setHi(0x05);
		cpu.hl.set16(0x3000);
		cpu.bc.set16(0x0002);
		machine.memoryBus().write(0x3000, (byte) 0x05);

		run(0xA9); // CPD

		assertEquals(0x2FFF, cpu.hl.get16());
	}

	// ---- CPIR / CPDR ----

	@Test
	public void cpirStopsEarlyOnceAMatchIsFoundRatherThanRunningToTheEndOfBc() throws Exception {
		cpu.af.setHi(0x33);
		cpu.hl.set16(0x3000);
		cpu.bc.set16(0x0005);
		machine.memoryBus().write(0x3000, (byte) 0x11);
		machine.memoryBus().write(0x3001, (byte) 0x22);
		machine.memoryBus().write(0x3002, (byte) 0x33); // the match, on the 3rd byte of 5
		machine.memoryBus().write(0x3003, (byte) 0x44);
		machine.memoryBus().write(0x3004, (byte) 0x55);

		run(0xB1); // CPIR

		assertEquals(0x3003, cpu.hl.get16(), "HL stopped right after the matching byte, not at the end of the whole block");
		assertEquals(0x0002, cpu.bc.get16(), "BC still has 2 left - the loop stopped because of the match, not because BC ran out");
		assertEquals(Flags.Z | Flags.PV | Flags.N, cpu.af.lo());
	}

	@Test
	public void cpirRunsToTheEndOfBcWhenNoMatchExists() throws Exception {
		cpu.af.setHi(0x99);
		cpu.hl.set16(0x3000);
		cpu.bc.set16(0x0003);
		machine.memoryBus().write(0x3000, (byte) 0x11);
		machine.memoryBus().write(0x3001, (byte) 0x22);
		machine.memoryBus().write(0x3002, (byte) 0x33);

		run(0xB1); // CPIR

		assertEquals(0x3003, cpu.hl.get16());
		assertEquals(0x0000, cpu.bc.get16());
		assertEquals(0, cpu.af.lo() & Flags.Z, "no byte matched, so the final comparison left Z clear");
	}

	@Test
	public void cpdrStepsHlBackwardWhileRepeating() throws Exception {
		cpu.af.setHi(0x33);
		cpu.hl.set16(0x3002);
		cpu.bc.set16(0x0003);
		machine.memoryBus().write(0x3000, (byte) 0x33); // the match, on the last byte scanned
		machine.memoryBus().write(0x3001, (byte) 0x22);
		machine.memoryBus().write(0x3002, (byte) 0x11);

		run(0xB9); // CPDR

		assertEquals(0x2FFF, cpu.hl.get16());
		assertEquals(0x0000, cpu.bc.get16());
	}
}
