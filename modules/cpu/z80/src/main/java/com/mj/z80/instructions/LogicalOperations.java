package com.mj.z80.instructions;

import com.mj.z80.Flags;
import com.mj.z80.Z80CPU;

/** AND, OR, and XOR's math and flag computation - a genuinely different
 * shape from ArithmeticOperations: no carry in or out, P/V reflects the
 * result's parity rather than two's-complement overflow, and H follows a
 * documented, fixed-per-operation rule rather than being computed from
 * the operands at all: AND always sets H=1; OR and XOR always clear it.
 * That's not a guess or an oversight - it's how real Z80 hardware's own
 * flag table defines these three, despite there being no actual half-
 * carry concept in a bitwise operation. N and C are always cleared for
 * all three (true of every bitwise logic op, not operation-specific). */
final class LogicalOperations {
	private LogicalOperations() {
	}

	static void and(Z80CPU cpu, int operand) {
		apply(cpu, cpu.af.hi() & operand, true);
	}

	static void or(Z80CPU cpu, int operand) {
		apply(cpu, cpu.af.hi() | operand, false);
	}

	static void xor(Z80CPU cpu, int operand) {
		apply(cpu, cpu.af.hi() ^ operand, false);
	}

	private static void apply(Z80CPU cpu, int result, boolean halfCarry) {
		int flags = 0;
		if ((result & 0x80) != 0) {
			flags |= Flags.S;
		}
		if (result == 0) {
			flags |= Flags.Z;
		}
		if (halfCarry) {
			flags |= Flags.H;
		}
		if (hasEvenParity(result)) {
			flags |= Flags.PV;
		}
		cpu.af.setLo(flags);
		cpu.af.setHi(result);
	}

	/** P/V doubles as "parity" for logic ops - set when the result has an
	 * even number of 1 bits (0 counts as even, so a zero result always has
	 * P/V set alongside Z - a real, slightly surprising but mathematically
	 * consistent detail, not a special case needed in code). */
	private static boolean hasEvenParity(int result) {
		return Integer.bitCount(result & 0xff) % 2 == 0;
	}
}
