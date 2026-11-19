package com.mj.z80.instructions;

import com.mj.z80.Z80CPU;
import com.mj.z80.Z80Instruction;

/** RRA ($1F) - rotates A right through carry (bit 0 -> C, old C -> bit 7).
 * See AccumulatorRotateOperations for the flag shape. */
public final class RotateRightAccumulator implements Z80Instruction {
	public static final RotateRightAccumulator INSTANCE = new RotateRightAccumulator();

	private RotateRightAccumulator() {
	}

	@Override
	public void execute(Z80CPU cpu) {
		AccumulatorRotateOperations.rra(cpu);
	}
}
