package com.mj.z80.instructions;

import com.mj.z80.Flags;
import com.mj.z80.Z80CPU;
import com.mj.z80.Z80Instruction;

/** CCF ($3F) - complements (inverts) the carry flag. A real, documented
 * quirk: H is set from the OLD carry value (before complementing), not
 * cleared like most "H=0" instructions in this family; N is cleared;
 * S/Z/P-V are left exactly as they were. Found missing 2026-11-19
 * alongside SCF, RLCA/RRCA/RLA/RRA, and ADD IX/IY,rr - all genuinely
 * documented instructions SDCC's own generated code uses routinely, never
 * exercised by any hand-written test program before now. */
public final class ComplementCarryFlag implements Z80Instruction {
	public static final ComplementCarryFlag INSTANCE = new ComplementCarryFlag();

	private ComplementCarryFlag() {
	}

	@Override
	public void execute(Z80CPU cpu) {
		boolean oldCarry = (cpu.af.lo() & Flags.C) != 0;
		int flags = cpu.af.lo() & (Flags.S | Flags.Z | Flags.PV);
		if (oldCarry) {
			flags |= Flags.H;
		} else {
			flags |= Flags.C;
		}
		cpu.af.setLo(flags);
	}
}
