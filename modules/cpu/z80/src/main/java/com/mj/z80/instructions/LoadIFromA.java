package com.mj.z80.instructions;

import com.mj.z80.Z80CPU;
import com.mj.z80.Z80Instruction;

/** LD I,A (ED $47) - copies A into the interrupt vector register I.
 * Unlike LD A,I (not implemented yet - see Z80CPU's comment on i/r), this
 * direction never affects any flag. */
public final class LoadIFromA implements Z80Instruction {
	public static final LoadIFromA INSTANCE = new LoadIFromA();

	private LoadIFromA() {
	}

	@Override
	public void execute(Z80CPU cpu) {
		cpu.i = cpu.af.hi();
	}
}
