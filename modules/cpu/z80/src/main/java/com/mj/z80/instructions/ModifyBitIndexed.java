package com.mj.z80.instructions;

import com.mj.exceptions.DeviceUnavailable;
import com.mj.exceptions.ROException;
import com.mj.exceptions.illegalAddressException;
import com.mj.z80.IndexRegister;
import com.mj.z80.Z80CPU;
import com.mj.z80.Z80Instruction;

/** RES b,(IX+d)/(IY+d) / SET b,(IX+d)/(IY+d) - the DD-CB/FD-CB indexed
 * tier, same cpu.pendingDisplacement convention as BitTestIndexed/
 * RotateShiftIndexed. Affects no flag at all. */
public final class ModifyBitIndexed implements Z80Instruction {
	private final int bitIndex;
	private final boolean setBit;
	private final IndexRegister index;

	public ModifyBitIndexed(int bitIndex, boolean setBit, IndexRegister index) {
		this.bitIndex = bitIndex;
		this.setBit = setBit;
		this.index = index;
	}

	@Override
	public void execute(Z80CPU cpu) throws illegalAddressException, DeviceUnavailable, ROException {
		int address = (index.get(cpu).get16() + cpu.pendingDisplacement) & 0xffff;
		int value = cpu.memoryBus.read(address) & 0xff;
		int result = setBit ? BitOperations.set(bitIndex, value) : BitOperations.res(bitIndex, value);
		cpu.memoryBus.write(address, (byte) result);
	}
}
