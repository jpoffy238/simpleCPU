package com.mj.z80.instructions;

import com.mj.z80.Flags;
import com.mj.z80.Z80CPU;

/** Shared 8-bit add/subtract-with-carry math for ADD, ADC, SUB, SBC, and
 * CP - all five share the exact same arithmetic shape (A combined with an
 * operand and an optional carry-in), differing only in whether carry-in
 * is included, whether it's addition or subtraction, and (CP alone)
 * whether the result is actually written back into A. AND/OR/XOR are a
 * genuinely different shape (no carry, parity instead of two's-complement
 * overflow, a per-operation H rule) and get their own math - see
 * LogicalOperations - not this class. */
final class ArithmeticOperations {
	private ArithmeticOperations() {
	}

	/** Computes A (+/-) operand (+/-) carry-in, sets S/Z/H/P-V/N/C from the
	 * result, and (unless CP, which passes writeBack=false) writes the
	 * result back into A. Returns the result either way, since nothing
	 * currently needs it, but a caller plausibly could. */
	static int addSub(Z80CPU cpu, int operand, boolean withCarry, boolean subtract, boolean writeBack) {
		int a = cpu.af.hi();
		int carryIn = withCarry && (cpu.af.lo() & Flags.C) != 0 ? 1 : 0;
		int raw = subtract ? a - operand - carryIn : a + operand + carryIn;
		int result = raw & 0xff;

		int halfRaw = subtract
				? (a & 0xf) - (operand & 0xf) - carryIn
				: (a & 0xf) + (operand & 0xf) + carryIn;
		boolean halfCarry = subtract ? halfRaw < 0 : halfRaw > 0xf;
		boolean carry = subtract ? raw < 0 : raw > 0xff;
		// Two's-complement overflow: ADD overflows when the operands share a
		// sign but the result doesn't match it; SUB (a - operand, treated as
		// a + (-operand)) overflows when the operands have DIFFERENT signs
		// and the result doesn't match the minuend's sign.
		boolean overflow = subtract
				? ((a ^ operand) & 0x80) != 0 && ((a ^ result) & 0x80) != 0
				: ((a ^ operand) & 0x80) == 0 && ((a ^ result) & 0x80) != 0;

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
		if (overflow) {
			flags |= Flags.PV;
		}
		if (subtract) {
			flags |= Flags.N;
		}
		if (carry) {
			flags |= Flags.C;
		}
		cpu.af.setLo(flags);

		if (writeBack) {
			cpu.af.setHi(result);
		}
		return result;
	}
}
