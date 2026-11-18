package com.mj.z80.instructions;

import com.mj.z80.Flags;
import com.mj.z80.Z80CPU;

/** Shared math for the CB-prefixed BIT/RES/SET family. BIT is read-only
 * (tests one bit, writes nothing back) and sets flags; RES/SET write
 * back a modified byte and affect NO flag at all - a third "no flags"
 * family alongside 16-bit INC/DEC and PUSH/POP. */
final class BitOperations {
	private BitOperations() {
	}

	/** Z is set when the tested bit is CLEAR - "the bit tested was zero",
	 * the opposite sense from most other uses of Z. H is always set - a
	 * documented quirk, same spirit as AND always setting it. S only has
	 * a documented meaning when testing bit 7 (it then reflects whether
	 * that bit is set); for every other bit position, and for C and P/V
	 * always, real hardware's own value is officially unaffected/unknown,
	 * so this leaves them exactly as they already were rather than
	 * guessing at a derived value. */
	static void bit(Z80CPU cpu, int bitIndex, int operand) {
		boolean bitSet = (operand & (1 << bitIndex)) != 0;
		int flags = cpu.af.lo() & (Flags.C | Flags.PV | Flags.S); // preserved by default
		if (bitIndex == 7) {
			flags = bitSet ? (flags | Flags.S) : (flags & ~Flags.S);
		}
		if (!bitSet) {
			flags |= Flags.Z;
		}
		flags |= Flags.H;
		cpu.af.setLo(flags);
	}

	static int res(int bitIndex, int operand) {
		return operand & ~(1 << bitIndex);
	}

	static int set(int bitIndex, int operand) {
		return operand | (1 << bitIndex);
	}
}
