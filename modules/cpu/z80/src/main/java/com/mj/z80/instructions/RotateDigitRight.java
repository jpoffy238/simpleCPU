package com.mj.z80.instructions;

import com.mj.exceptions.DeviceUnavailable;
import com.mj.exceptions.ROException;
import com.mj.exceptions.illegalAddressException;
import com.mj.z80.Z80CPU;
import com.mj.z80.Z80Instruction;

/** RRD (ED $67) - see DigitRotateOperations. */
public final class RotateDigitRight implements Z80Instruction {
	public static final RotateDigitRight INSTANCE = new RotateDigitRight();

	private RotateDigitRight() {
	}

	@Override
	public void execute(Z80CPU cpu) throws illegalAddressException, DeviceUnavailable, ROException {
		DigitRotateOperations.rotateRight(cpu);
	}
}
