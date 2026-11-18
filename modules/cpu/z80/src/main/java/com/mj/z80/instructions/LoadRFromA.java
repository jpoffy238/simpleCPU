package com.mj.z80.instructions;

import com.mj.z80.Z80CPU;
import com.mj.z80.Z80Instruction;

/** LD R,A (ED $4F) - copies A into the memory-refresh register R. Unlike
 * LD A,R (not implemented yet - see Z80CPU's comment on i/r), this
 * direction never affects any flag. */
public final class LoadRFromA implements Z80Instruction {
	public static final LoadRFromA INSTANCE = new LoadRFromA();

	private LoadRFromA() {
	}

	@Override
	public void execute(Z80CPU cpu) {
		cpu.r = cpu.af.hi();
	}
}
