package com.mj.z80.instructions;

import com.mj.z80.Flags;
import com.mj.z80.Z80CPU;

/** Shared bit math and flag computation for the unprefixed RLCA/RRCA/RLA/
 * RRA ($07/$0F/$17/$1F) - the same rotate shapes as the CB-prefixed RLC/
 * RRC/RL/RR (see RotateShiftOperations), but a genuinely different flag
 * shape: this family leaves S/Z/P-V exactly as they were (CB-prefixed
 * rotates set them from the result), only setting H=0/N=0/C from whichever
 * bit rotated out. A real, documented, easy-to-get-wrong distinction - a
 * gap found 2026-11-19 porting bdos.c/ccp.c to Z80 (SDCC uses these
 * constantly for byte-swap/multiply-by-two idioms), same posture as
 * AddIndexRegisterPair's own gap found the same day. */
final class AccumulatorRotateOperations {
	private AccumulatorRotateOperations() {
	}

	static void rlca(Z80CPU cpu) {
		int a = cpu.af.hi();
		int carryOut = (a >> 7) & 1;
		cpu.af.setHi(((a << 1) | carryOut) & 0xff);
		setFlags(cpu, carryOut);
	}

	static void rrca(Z80CPU cpu) {
		int a = cpu.af.hi();
		int carryOut = a & 1;
		cpu.af.setHi((a >> 1) | (carryOut << 7));
		setFlags(cpu, carryOut);
	}

	static void rla(Z80CPU cpu) {
		int a = cpu.af.hi();
		int carryIn = (cpu.af.lo() & Flags.C) != 0 ? 1 : 0;
		int carryOut = (a >> 7) & 1;
		cpu.af.setHi(((a << 1) | carryIn) & 0xff);
		setFlags(cpu, carryOut);
	}

	static void rra(Z80CPU cpu) {
		int a = cpu.af.hi();
		int carryIn = (cpu.af.lo() & Flags.C) != 0 ? 0x80 : 0;
		int carryOut = a & 1;
		cpu.af.setHi((a >> 1) | carryIn);
		setFlags(cpu, carryOut);
	}

	private static void setFlags(Z80CPU cpu, int carryOut) {
		int flags = cpu.af.lo() & (Flags.S | Flags.Z | Flags.PV);
		if (carryOut != 0) {
			flags |= Flags.C;
		}
		cpu.af.setLo(flags);
	}
}
