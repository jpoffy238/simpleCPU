package com.mj.z80.instructions;

import com.mj.exceptions.DeviceUnavailable;
import com.mj.exceptions.illegalAddressException;
import com.mj.z80.RegisterPair16;
import com.mj.z80.Z80CPU;
import com.mj.z80.Z80Instruction;

/** LD dd,nn - loads a 16-bit literal (low byte then high byte - the Z80's
 * standard little-endian order for every 16-bit value) into BC, DE, HL,
 * or SP. Never affects any flag. */
public final class LoadRegisterPairImmediate implements Z80Instruction {
	private final RegisterPair16 destination;

	public LoadRegisterPairImmediate(RegisterPair16 destination) {
		this.destination = destination;
	}

	@Override
	public void execute(Z80CPU cpu) throws illegalAddressException, DeviceUnavailable {
		int low = cpu.fetchByte() & 0xff;
		int high = cpu.fetchByte() & 0xff;
		destination.set16(cpu, low | (high << 8));
	}
}
