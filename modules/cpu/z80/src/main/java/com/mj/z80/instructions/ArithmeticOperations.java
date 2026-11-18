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

	/** NEG (ED $44) - A = 0 - A (two's-complement negation). Same flag
	 * shape as a subtraction, just with 0 as the implicit left operand
	 * rather than A itself - not expressible through addSub() as-is,
	 * which always reads A as the left operand, so this duplicates its
	 * formulas by hand with 0 substituted in, rather than reshaping
	 * addSub() for one caller. */
	static int negate(Z80CPU cpu) {
		int a = cpu.af.hi();
		int raw = -a;
		int result = raw & 0xff;
		boolean halfCarry = -(a & 0xf) < 0;
		boolean carry = raw < 0;
		boolean overflow = (a & 0x80) != 0 && (result & 0x80) != 0;

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
		flags |= Flags.N;
		if (carry) {
			flags |= Flags.C;
		}
		cpu.af.setLo(flags);
		cpu.af.setHi(result);
		return result;
	}

	/** ADC HL,rr / SBC HL,rr (ED-prefixed) - the 16-bit counterpart of
	 * addSub() above, always including the carry flag (there's no
	 * carry-less ED-prefixed form the way plain ADD/SUB have one at
	 * 8 bits - that's the separate, simpler, flags-light ADD HL,rr,
	 * unprefixed and not part of this family) and always writing the
	 * result back into HL (there's no 16-bit CP to need a writeBack
	 * flag for). Same formulas as addSub(), scaled from 8 to 16 bits
	 * (nibble-boundary half-carry becomes byte-boundary, bit 7's sign
	 * becomes bit 15's). */
	static int addSubHL(Z80CPU cpu, int operand, boolean subtract) {
		int hl = cpu.hl.get16();
		int carryIn = (cpu.af.lo() & Flags.C) != 0 ? 1 : 0;
		int raw = subtract ? hl - operand - carryIn : hl + operand + carryIn;
		int result = raw & 0xffff;

		int halfRaw = subtract
				? (hl & 0xfff) - (operand & 0xfff) - carryIn
				: (hl & 0xfff) + (operand & 0xfff) + carryIn;
		boolean halfCarry = subtract ? halfRaw < 0 : halfRaw > 0xfff;
		boolean carry = subtract ? raw < 0 : raw > 0xffff;
		boolean overflow = subtract
				? ((hl ^ operand) & 0x8000) != 0 && ((hl ^ result) & 0x8000) != 0
				: ((hl ^ operand) & 0x8000) == 0 && ((hl ^ result) & 0x8000) != 0;

		int flags = 0;
		if ((result & 0x8000) != 0) {
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

		cpu.hl.set16(result);
		return result;
	}
}
