package com.mj.z80.instructions;

import com.mj.exceptions.DeviceUnavailable;
import com.mj.exceptions.ROException;
import com.mj.exceptions.illegalAddressException;
import com.mj.z80.Z80CPU;
import com.mj.z80.Z80Instruction;

/** LD (BC),A ($02) / LD (DE),A ($12) - stores A to the address held in BC
 * or DE. See LoadAFromPairMemory's class comment, including why this takes
 * a selector rather than a captured RegisterPair. Never affects any
 * flag. */
public final class StoreAToPairMemory implements Z80Instruction {
	private final LoadAFromPairMemory.Selector dest;

	public StoreAToPairMemory(LoadAFromPairMemory.Selector dest) {
		this.dest = dest;
	}

	@Override
	public void execute(Z80CPU cpu) throws illegalAddressException, DeviceUnavailable, ROException {
		cpu.memoryBus.write(dest.get(cpu).get16(), (byte) cpu.af.hi());
	}
}
