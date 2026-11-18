package com.mj.z80.instructions;

import com.mj.exceptions.DeviceUnavailable;
import com.mj.exceptions.ROException;
import com.mj.exceptions.illegalAddressException;
import com.mj.z80.IndexRegister;
import com.mj.z80.Z80CPU;
import com.mj.z80.Z80Instruction;

/** DEC (IX+d) / DEC (IY+d) - same flag rules as DecrementMemory, applied
 * to the byte at (index register + a signed displacement). */
public final class DecrementIndexed implements Z80Instruction {
	private final IndexRegister index;

	public DecrementIndexed(IndexRegister index) {
		this.index = index;
	}

	@Override
	public void execute(Z80CPU cpu) throws illegalAddressException, DeviceUnavailable, ROException {
		byte displacement = cpu.fetchByte();
		int address = (index.get(cpu).get16() + displacement) & 0xffff;
		int value = cpu.memoryBus.read(address) & 0xff;
		cpu.memoryBus.write(address, (byte) IncDecOperations.decrement(cpu, value));
	}
}
