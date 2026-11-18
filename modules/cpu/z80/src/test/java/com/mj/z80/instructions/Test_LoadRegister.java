package com.mj.z80.instructions;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.mj.z80.Register8;
import com.mj.z80.Z80CPU;
import com.mj.z80.Z80InstructionDecoder;
import com.mj.z80.Z80Machine;

/** LD r,r' - the whole register-to-register family (see LoadRegisterTable).
 * Each test loads a single opcode byte, runs it, and checks three things
 * real Z80 hardware guarantees: the destination takes on the source
 * register's value, the source register itself is left alone (this isn't
 * a swap), and no flag is touched - true of every 8-bit LD, not just
 * these. A handful of real, independently-known opcodes are pinned
 * explicitly (one per destination register) as a cross-check against
 * LoadRegisterTable's own formula - the sweep test below only proves that
 * formula is internally consistent with itself, not that it matches real
 * Z80 hardware, so pinning a few values against the actual Zilog-documented
 * opcodes is what actually catches a transposed ddd/sss bit-shift. */
public class Test_LoadRegister {
	private Z80Machine machine;
	private Z80CPU cpu;

	@BeforeEach
	public void setup() {
		machine = new Z80Machine();
		Z80InstructionDecoder decoder = new Z80InstructionDecoder();
		LoadRegisterTable.populate(decoder);
		cpu = new Z80CPU(machine.memoryBus(), machine.portBus(), decoder);
	}

	private void assertLoadMoves(int opcode, Register8 destination, Register8 source) throws Exception {
		destination.set(cpu, 0x00); // destination starts different from the value the source is about to get
		source.set(cpu, 0x42);
		cpu.af.setLo(0x55); // flags sentinel - LD must never touch it
		machine.memoryBus().write(0x1000, (byte) opcode);
		cpu.pc = 0x1000;

		cpu.step();

		assertEquals(0x42, destination.get(cpu), "destination should now hold the source register's value");
		assertEquals(0x42, source.get(cpu), "the source register itself must be left untouched - this isn't a swap");
		assertEquals(0x55, cpu.af.lo(), "LD r,r' must never affect flags");
		assertEquals(0x1001, cpu.pc);
	}

	@Test
	public void ldAB() throws Exception {
		assertLoadMoves(0x78, Register8.A, Register8.B);
	}

	@Test
	public void ldAC() throws Exception {
		assertLoadMoves(0x79, Register8.A, Register8.C);
	}

	@Test
	public void ldAD() throws Exception {
		assertLoadMoves(0x7A, Register8.A, Register8.D);
	}

	@Test
	public void ldAE() throws Exception {
		assertLoadMoves(0x7B, Register8.A, Register8.E);
	}

	@Test
	public void ldAH() throws Exception {
		assertLoadMoves(0x7C, Register8.A, Register8.H);
	}

	@Test
	public void ldAL() throws Exception {
		assertLoadMoves(0x7D, Register8.A, Register8.L);
	}

	@Test
	public void ldAAIsARealOpcodeNotJustASkippedSlot() throws Exception {
		assertLoadMoves(0x7F, Register8.A, Register8.A);
	}

	// One pinned, independently-known real opcode per remaining destination
	// register - see this class's own comment on why the sweep test below
	// isn't enough on its own.

	@Test
	public void ldBB() throws Exception {
		assertLoadMoves(0x40, Register8.B, Register8.B);
	}

	@Test
	public void ldBA() throws Exception {
		assertLoadMoves(0x47, Register8.B, Register8.A);
	}

	@Test
	public void ldCA() throws Exception {
		assertLoadMoves(0x4F, Register8.C, Register8.A);
	}

	@Test
	public void ldDB() throws Exception {
		assertLoadMoves(0x50, Register8.D, Register8.B);
	}

	@Test
	public void ldEA() throws Exception {
		assertLoadMoves(0x5F, Register8.E, Register8.A);
	}

	@Test
	public void ldHB() throws Exception {
		assertLoadMoves(0x60, Register8.H, Register8.B);
	}

	@Test
	public void ldLA() throws Exception {
		assertLoadMoves(0x6F, Register8.L, Register8.A);
	}

	/** Every one of the 49 valid register-to-register combinations, computed
	 * via the exact same `01 ddd sss` formula LoadRegisterTable itself uses
	 * - a regression net for the whole family's wiring (right opcode, right
	 * value transfer, source untouched, flags untouched, PC advance), on
	 * top of (not instead of) the pinned spot checks above. */
	@Test
	public void everyRegisterToRegisterCombinationDispatchesCorrectly() throws Exception {
		for (Register8 destination : Register8.values()) {
			for (Register8 source : Register8.values()) {
				int opcode = 0x40 | (destination.code() << 3) | source.code();
				assertLoadMoves(opcode, destination, source);
			}
		}
	}
}
