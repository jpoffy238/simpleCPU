package com.mj.z80.instructions;

import com.mj.z80.Flags;
import com.mj.z80.Z80CPU;

/** Shared flag computation for LD A,I / LD A,R - S/Z from the value, H=0,
 * P/V=IFF2 (a real, documented quirk: this is the one place P/V reflects
 * the interrupt-enable flip-flop rather than parity or overflow), N=0, C
 * preserved. */
final class InterruptOperations {
	private InterruptOperations() {
	}

	static void setLoadFlags(Z80CPU cpu, int value) {
		int flags = cpu.af.lo() & Flags.C;
		if ((value & 0x80) != 0) {
			flags |= Flags.S;
		}
		if (value == 0) {
			flags |= Flags.Z;
		}
		if (cpu.iff2) {
			flags |= Flags.PV;
		}
		cpu.af.setLo(flags);
	}
}
