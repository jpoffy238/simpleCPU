package com.mj.z80.instructions;

import com.mj.z80.Z80CPU;
import com.mj.z80.Z80Instruction;

/** RLCA ($07) - rotates A left circularly (bit 7 wraps into bit 0, and
 * into C too). See AccumulatorRotateOperations for the flag shape. */
public final class RotateLeftAccumulatorCircular implements Z80Instruction {
	public static final RotateLeftAccumulatorCircular INSTANCE = new RotateLeftAccumulatorCircular();

	private RotateLeftAccumulatorCircular() {
	}

	@Override
	public void execute(Z80CPU cpu) {
		AccumulatorRotateOperations.rlca(cpu);
	}
}
