package com.mj.z80.instructions;

import com.mj.exceptions.DeviceUnavailable;
import com.mj.exceptions.illegalAddressException;
import com.mj.z80.IndexRegister;
import com.mj.z80.Z80CPU;
import com.mj.z80.Z80Instruction;

/** BIT b,(IX+d) / BIT b,(IY+d) - the DD-CB/FD-CB indexed tier. Same
 * cpu.pendingDisplacement convention as RotateShiftIndexed - the
 * decoder already read the displacement before this instruction could
 * even be identified. */
public final class BitTestIndexed implements Z80Instruction {
	private final int bitIndex;
	private final IndexRegister index;

	public BitTestIndexed(int bitIndex, IndexRegister index) {
		this.bitIndex = bitIndex;
		this.index = index;
	}

	@Override
	public void execute(Z80CPU cpu) throws illegalAddressException, DeviceUnavailable {
		int address = (index.get(cpu).get16() + cpu.pendingDisplacement) & 0xffff;
		int value = cpu.memoryBus.read(address) & 0xff;
		BitOperations.bit(cpu, bitIndex, value);
	}
}
