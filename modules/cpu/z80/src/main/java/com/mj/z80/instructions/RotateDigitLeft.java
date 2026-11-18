package com.mj.z80.instructions;

import com.mj.exceptions.DeviceUnavailable;
import com.mj.exceptions.ROException;
import com.mj.exceptions.illegalAddressException;
import com.mj.z80.Z80CPU;
import com.mj.z80.Z80Instruction;

/** RLD (ED $6F) - see DigitRotateOperations. */
public final class RotateDigitLeft implements Z80Instruction {
	public static final RotateDigitLeft INSTANCE = new RotateDigitLeft();

	private RotateDigitLeft() {
	}

	@Override
	public void execute(Z80CPU cpu) throws illegalAddressException, DeviceUnavailable, ROException {
		DigitRotateOperations.rotateLeft(cpu);
	}
}
