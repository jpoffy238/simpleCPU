package com.mj.z80.instructions;

import com.mj.z80.Flags;
import com.mj.z80.Z80CPU;
import com.mj.z80.Z80Instruction;

/** DAA ($27) - decimal-adjusts A after a BCD ADD/ADC/SUB/SBC, so two
 * valid packed-BCD bytes added or subtracted with ordinary binary
 * arithmetic come back out as a valid packed-BCD result. Depends on N
 * (was the last op an add or a subtract) plus the H/C flags that op left
 * behind, so it only makes sense immediately after one of them - exactly
 * how real CP/M-era BCD arithmetic routines actually used it.
 *
 * The correction (add 0x06 and/or 0x60, or subtract them if N is set) is
 * the same regardless of add/subtract: 0x06 is needed whenever H was set
 * or A's low nibble is already out of BCD range (>9); 0x60 is needed
 * whenever C was set or A itself is >0x99. New H is only meaningful on
 * the subtract side (set when the old H was set AND the low nibble was
 * <6 - i.e. the low-nibble borrow would itself have borrowed from the
 * high nibble); on the add side it's just "did the low nibble overflow."
 * New C latches once set by this same 0x60 condition - DAA itself can
 * only ever set C, never clear a C that was already set, since a correct
 * BCD result that needed correcting upward can't also need to carry back
 * down. */
public final class DecimalAdjustAccumulator implements Z80Instruction {
	public static final DecimalAdjustAccumulator INSTANCE = new DecimalAdjustAccumulator();

	private DecimalAdjustAccumulator() {
	}

	@Override
	public void execute(Z80CPU cpu) {
		int a = cpu.af.hi();
		int f = cpu.af.lo();
		boolean carry = (f & Flags.C) != 0;
		boolean halfCarry = (f & Flags.H) != 0;
		boolean subtract = (f & Flags.N) != 0;

		int correction = 0;
		if (halfCarry || (a & 0x0f) > 9) {
			correction |= 0x06;
		}
		if (carry || a > 0x99) {
			correction |= 0x60;
		}

		int result = (subtract ? a - correction : a + correction) & 0xff;

		boolean newHalfCarry = subtract ? (halfCarry && (a & 0x0f) < 6) : (a & 0x0f) > 9;
		boolean newCarry = carry || a > 0x99;

		int flags = f & Flags.N; // N itself is never affected by DAA
		if ((result & 0x80) != 0) {
			flags |= Flags.S;
		}
		if (result == 0) {
			flags |= Flags.Z;
		}
		if (newHalfCarry) {
			flags |= Flags.H;
		}
		if (Integer.bitCount(result) % 2 == 0) {
			flags |= Flags.PV;
		}
		if (newCarry) {
			flags |= Flags.C;
		}

		cpu.af.setHi(result);
		cpu.af.setLo(flags);
	}
}
