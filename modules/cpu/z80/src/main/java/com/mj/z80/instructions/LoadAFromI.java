package com.mj.z80.instructions;

import com.mj.z80.Z80CPU;
import com.mj.z80.Z80Instruction;

/** LD A,I (ED $57) - copies the interrupt vector register I into A and
 * sets flags from the result (see InterruptOperations) - the one LD that
 * reads interrupt state, which is why it was held back until IFF2
 * existed. */
public final class LoadAFromI implements Z80Instruction {
	public static final LoadAFromI INSTANCE = new LoadAFromI();

	private LoadAFromI() {
	}

	@Override
	public void execute(Z80CPU cpu) {
		int value = cpu.i & 0xff;
		cpu.af.setHi(value);
		InterruptOperations.setLoadFlags(cpu, value);
	}
}
