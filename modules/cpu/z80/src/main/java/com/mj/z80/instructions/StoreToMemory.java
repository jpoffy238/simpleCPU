package com.mj.z80.instructions;

import com.mj.exceptions.DeviceUnavailable;
import com.mj.exceptions.ROException;
import com.mj.exceptions.illegalAddressException;
import com.mj.z80.Register8;
import com.mj.z80.Z80CPU;
import com.mj.z80.Z80Instruction;

/** LD (HL),r - stores one 8-bit register's value at the address HL points
 * at. Same (IX+d)/(IY+d) scoping note as LoadFromMemory. Never affects any
 * flag. */
public final class StoreToMemory implements Z80Instruction {
	private final Register8 source;

	public StoreToMemory(Register8 source) {
		this.source = source;
	}

	@Override
	public void execute(Z80CPU cpu) throws illegalAddressException, DeviceUnavailable, ROException {
		cpu.memoryBus.write(cpu.hl.get16(), (byte) source.get(cpu));
	}
}
