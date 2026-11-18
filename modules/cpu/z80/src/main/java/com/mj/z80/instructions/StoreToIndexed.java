package com.mj.z80.instructions;

import com.mj.exceptions.DeviceUnavailable;
import com.mj.exceptions.ROException;
import com.mj.exceptions.illegalAddressException;
import com.mj.z80.IndexRegister;
import com.mj.z80.Register8;
import com.mj.z80.Z80CPU;
import com.mj.z80.Z80Instruction;

/** LD (IX+d),r / LD (IY+d),r - stores one 8-bit register's value at
 * (index register + a signed displacement). See LoadFromIndexed's class
 * comment for the displacement/sign-extension and no-H/L-entanglement
 * notes, both equally true here. Never affects any flag. */
public final class StoreToIndexed implements Z80Instruction {
	private final IndexRegister index;
	private final Register8 source;

	public StoreToIndexed(IndexRegister index, Register8 source) {
		this.index = index;
		this.source = source;
	}

	@Override
	public void execute(Z80CPU cpu) throws illegalAddressException, DeviceUnavailable, ROException {
		byte displacement = cpu.fetchByte();
		int address = (index.get(cpu).get16() + displacement) & 0xffff;
		cpu.memoryBus.write(address, (byte) source.get(cpu));
	}
}
