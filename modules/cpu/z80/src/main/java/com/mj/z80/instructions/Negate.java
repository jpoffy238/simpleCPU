package com.mj.z80.instructions;

import com.mj.z80.Z80CPU;
import com.mj.z80.Z80Instruction;

/** NEG (ED $44) - A = 0 - A. Stateless, so one shared instance covers
 * every call. */
public final class Negate implements Z80Instruction {
	public static final Negate INSTANCE = new Negate();

	private Negate() {
	}

	@Override
	public void execute(Z80CPU cpu) {
		ArithmeticOperations.negate(cpu);
	}
}
