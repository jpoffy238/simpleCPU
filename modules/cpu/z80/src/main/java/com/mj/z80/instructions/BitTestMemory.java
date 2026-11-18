package com.mj.z80.instructions;

import com.mj.exceptions.DeviceUnavailable;
import com.mj.exceptions.illegalAddressException;
import com.mj.z80.Z80CPU;
import com.mj.z80.Z80Instruction;

/** BIT b,(HL) (`01 bbb 110`) - tests a bit of the byte at [HL] without
 * modifying it. */
public final class BitTestMemory implements Z80Instruction {
	private final int bitIndex;

	public BitTestMemory(int bitIndex) {
		this.bitIndex = bitIndex;
	}

	@Override
	public void execute(Z80CPU cpu) throws illegalAddressException, DeviceUnavailable {
		int value = cpu.memoryBus.read(cpu.hl.get16()) & 0xff;
		BitOperations.bit(cpu, bitIndex, value);
	}
}
