package com.mj.z80.instructions;

import com.mj.z80.Z80CPU;
import com.mj.z80.Z80Instruction;

/** $00 unprefixed - does nothing. Stateless, so one shared instance covers
 * every call (same singleton-table-row convention the whole decoder is
 * built around). */
public final class NOP implements Z80Instruction {
	public static final NOP INSTANCE = new NOP();

	private NOP() {
	}

	@Override
	public void execute(Z80CPU cpu) {
	}
}
