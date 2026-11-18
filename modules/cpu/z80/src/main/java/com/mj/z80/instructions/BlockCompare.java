package com.mj.z80.instructions;

import com.mj.exceptions.DeviceUnavailable;
import com.mj.exceptions.illegalAddressException;
import com.mj.z80.Z80CPU;
import com.mj.z80.Z80Instruction;

/** CPI (direction +1, $A1) / CPD (direction -1, $A9) - a single
 * block-compare step (see BlockOperations). */
public final class BlockCompare implements Z80Instruction {
	private final int direction;

	public BlockCompare(int direction) {
		this.direction = direction;
	}

	@Override
	public void execute(Z80CPU cpu) throws illegalAddressException, DeviceUnavailable {
		BlockOperations.compareStep(cpu, direction);
	}
}
