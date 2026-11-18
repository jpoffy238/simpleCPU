package com.mj.z80.instructions;

import com.mj.exceptions.DeviceUnavailable;
import com.mj.exceptions.ROException;
import com.mj.exceptions.illegalAddressException;
import com.mj.z80.Z80CPU;
import com.mj.z80.Z80Instruction;

/** LDI (direction +1, $A0) / LDD (direction -1, $A8) - a single block-copy
 * step (see BlockOperations). */
public final class BlockCopy implements Z80Instruction {
	private final int direction;

	public BlockCopy(int direction) {
		this.direction = direction;
	}

	@Override
	public void execute(Z80CPU cpu) throws illegalAddressException, DeviceUnavailable, ROException {
		BlockOperations.copyStep(cpu, direction);
	}
}
