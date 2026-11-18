package com.mj.z80.instructions;

import com.mj.z80.Flags;
import com.mj.z80.Z80CPU;

/** Shared 8-bit increment/decrement math and flag computation for INC r/
 * DEC r and their (HL)/(IX+d)/(IY+d) memory forms. A genuinely different
 * shape from ArithmeticOperations' ADD/SUB: no carry-in, and - a real,
 * easy-to-miss hardware quirk - the carry flag (C) is never touched at
 * all, unlike ADD/SUB which always set it from the result. Overflow
 * (P/V) only has one possible trigger each direction (0x7F->0x80
 * incrementing, 0x80->0x7F decrementing - the only values where crossing
 * the signed 8-bit boundary is even possible for a +/-1 step), not the
 * general two's-complement check ADD/SUB need for an arbitrary operand.
 * 16-bit INC/DEC (a different family entirely - see
 * IncrementRegisterPair/DecrementRegisterPair) affect no flag whatsoever,
 * so this class has nothing to do with them. */
final class IncDecOperations {
	private IncDecOperations() {
	}

	static int increment(Z80CPU cpu, int operand) {
		int result = (operand + 1) & 0xff;
		int flags = cpu.af.lo() & Flags.C; // preserve C - INC never touches it
		if ((result & 0x80) != 0) {
			flags |= Flags.S;
		}
		if (result == 0) {
			flags |= Flags.Z;
		}
		if ((operand & 0x0f) == 0x0f) {
			flags |= Flags.H;
		}
		if (operand == 0x7f) {
			flags |= Flags.PV;
		}
		cpu.af.setLo(flags);
		return result;
	}

	static int decrement(Z80CPU cpu, int operand) {
		int result = (operand - 1) & 0xff;
		int flags = (cpu.af.lo() & Flags.C) | Flags.N; // preserve C; DEC always sets N
		if ((result & 0x80) != 0) {
			flags |= Flags.S;
		}
		if (result == 0) {
			flags |= Flags.Z;
		}
		if ((operand & 0x0f) == 0x00) {
			flags |= Flags.H;
		}
		if (operand == 0x80) {
			flags |= Flags.PV;
		}
		cpu.af.setLo(flags);
		return result;
	}
}
