package com.mj.z80.instructions;

import com.mj.exceptions.DeviceUnavailable;
import com.mj.exceptions.ROException;
import com.mj.exceptions.illegalAddressException;
import com.mj.z80.Z80CPU;
import com.mj.z80.Z80Instruction;

/** RES b,(HL) / SET b,(HL) - see ModifyBitRegister's class comment; same
 * shared-class-via-boolean shape, applied to the byte at [HL]. Affects
 * no flag at all. */
public final class ModifyBitMemory implements Z80Instruction {
	private final int bitIndex;
	private final boolean setBit;

	public ModifyBitMemory(int bitIndex, boolean setBit) {
		this.bitIndex = bitIndex;
		this.setBit = setBit;
	}

	@Override
	public void execute(Z80CPU cpu) throws illegalAddressException, DeviceUnavailable, ROException {
		int address = cpu.hl.get16();
		int value = cpu.memoryBus.read(address) & 0xff;
		int result = setBit ? BitOperations.set(bitIndex, value) : BitOperations.res(bitIndex, value);
		cpu.memoryBus.write(address, (byte) result);
	}
}
