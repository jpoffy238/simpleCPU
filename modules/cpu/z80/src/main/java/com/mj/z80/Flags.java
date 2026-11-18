package com.mj.z80;

/** Bit masks for the F register (the low byte of AF) - S/Z/H/P-V/N/C. Each
 * instruction computes its own flag effects explicitly (see project notes
 * on why this doesn't generalize the way the 6502 side's register-throws-
 * an-exception-to-signal-Z/N pattern does); these constants are just the
 * bit positions real instructions read/write via af.lo()/af.setLo(). Bits 5
 * and 3 (the undocumented "X"/"Y" flags, real hardware copies of bits 13/11
 * of certain results) are deliberately not named here - out of scope for
 * now, same as every other undocumented behavior. */
public final class Flags {
	public static final int S = 0x80;
	public static final int Z = 0x40;
	public static final int H = 0x10;
	public static final int PV = 0x04;
	public static final int N = 0x02;
	public static final int C = 0x01;

	private Flags() {
	}
}
