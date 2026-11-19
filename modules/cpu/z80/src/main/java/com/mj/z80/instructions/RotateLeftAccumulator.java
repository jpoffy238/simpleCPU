package com.mj.z80.instructions;

import com.mj.z80.Z80CPU;
import com.mj.z80.Z80Instruction;

/** RLA ($17) - rotates A left through carry (bit 7 -> C, old C -> bit 0).
 * See AccumulatorRotateOperations for the flag shape. */
public final class RotateLeftAccumulator implements Z80Instruction {
	public static final RotateLeftAccumulator INSTANCE = new RotateLeftAccumulator();

	private RotateLeftAccumulator() {
	}

	@Override
	public void execute(Z80CPU cpu) {
		AccumulatorRotateOperations.rla(cpu);
	}
}
