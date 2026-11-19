package com.mj.z80.instructions;

import com.mj.z80.Flags;
import com.mj.z80.Z80CPU;
import com.mj.z80.Z80Instruction;

/** SCF ($37) - sets the carry flag. Clears H and N; S/Z/P-V are left
 * exactly as they were. Another real, documented instruction found
 * missing 2026-11-19 alongside CCF - see ComplementCarryFlag's own
 * comment. */
public final class SetCarryFlag implements Z80Instruction {
	public static final SetCarryFlag INSTANCE = new SetCarryFlag();

	private SetCarryFlag() {
	}

	@Override
	public void execute(Z80CPU cpu) {
		int flags = (cpu.af.lo() & (Flags.S | Flags.Z | Flags.PV)) | Flags.C;
		cpu.af.setLo(flags);
	}
}
