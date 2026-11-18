package com.mj.z80.instructions;

import com.mj.z80.Flags;
import com.mj.z80.Z80CPU;
import com.mj.z80.Z80Instruction;

/** CPL ($2F) - one's-complements A (every bit flipped). Always sets H
 * and N (a real, documented quirk, same spirit as AND always setting H);
 * S/Z/P-V/C are all left exactly as they were. */
public final class ComplementAccumulator implements Z80Instruction {
	public static final ComplementAccumulator INSTANCE = new ComplementAccumulator();

	private ComplementAccumulator() {
	}

	@Override
	public void execute(Z80CPU cpu) {
		int result = (~cpu.af.hi()) & 0xff;
		cpu.af.setHi(result);

		int flags = (cpu.af.lo() & (Flags.S | Flags.Z | Flags.PV | Flags.C)) | Flags.H | Flags.N;
		cpu.af.setLo(flags);
	}
}
