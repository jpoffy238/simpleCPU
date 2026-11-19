package com.mj.z80.instructions;

import com.mj.z80.Z80CPU;
import com.mj.z80.Z80Instruction;

/** RRCA ($0F) - rotates A right circularly (bit 0 wraps into bit 7, and
 * into C too). See AccumulatorRotateOperations for the flag shape. */
public final class RotateRightAccumulatorCircular implements Z80Instruction {
	public static final RotateRightAccumulatorCircular INSTANCE = new RotateRightAccumulatorCircular();

	private RotateRightAccumulatorCircular() {
	}

	@Override
	public void execute(Z80CPU cpu) {
		AccumulatorRotateOperations.rrca(cpu);
	}
}
