package com.mj.z80.instructions;

import com.mj.z80.Z80CPU;
import com.mj.z80.Z80Instruction;

/** LD A,R (ED $5F) - copies the memory-refresh register R into A and sets
 * flags the same way LD A,I does (see InterruptOperations). */
public final class LoadAFromR implements Z80Instruction {
	public static final LoadAFromR INSTANCE = new LoadAFromR();

	private LoadAFromR() {
	}

	@Override
	public void execute(Z80CPU cpu) {
		int value = cpu.r & 0xff;
		cpu.af.setHi(value);
		InterruptOperations.setLoadFlags(cpu, value);
	}
}
