package com.mj.z80.instructions;

import com.mj.exceptions.DeviceUnavailable;
import com.mj.exceptions.illegalAddressException;
import com.mj.z80.Register8;
import com.mj.z80.Z80CPU;
import com.mj.z80.Z80Instruction;

/** LD r,(HL) - loads the byte at the address HL points at into one 8-bit
 * register. (IX+d)/(IY+d) forms need a displacement, not just a plain
 * 16-bit pointer register, so they're a different instruction shape -
 * this class isn't generalized to cover them ahead of actually needing
 * to. Never affects any flag, same as every other LD. */
public final class LoadFromMemory implements Z80Instruction {
	private final Register8 destination;

	public LoadFromMemory(Register8 destination) {
		this.destination = destination;
	}

	@Override
	public void execute(Z80CPU cpu) throws illegalAddressException, DeviceUnavailable {
		destination.set(cpu, cpu.memoryBus.read(cpu.hl.get16()) & 0xff);
	}
}
