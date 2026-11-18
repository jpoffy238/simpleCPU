package com.mj.z80.instructions;

import com.mj.z80.Flags;
import com.mj.z80.Z80CPU;

/** Shared flag computation for IN r,(C) - S/Z from the byte read, H=0,
 * P/V=parity of the byte (same convention as the logic ALU family), N=0,
 * C left exactly as it was. IN A,(n) deliberately does NOT call this - a
 * real, documented asymmetry: the simple unprefixed form affects no flag
 * at all, only the ED-prefixed register form does. OUT never affects any
 * flag either way, so there's no corresponding output-side method here. */
final class PortOperations {
	private PortOperations() {
	}

	static void setInputFlags(Z80CPU cpu, int value) {
		int flags = cpu.af.lo() & Flags.C; // preserved
		if ((value & 0x80) != 0) {
			flags |= Flags.S;
		}
		if (value == 0) {
			flags |= Flags.Z;
		}
		if (Integer.bitCount(value & 0xff) % 2 == 0) {
			flags |= Flags.PV;
		}
		cpu.af.setLo(flags);
	}
}
