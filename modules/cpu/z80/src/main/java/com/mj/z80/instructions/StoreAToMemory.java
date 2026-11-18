package com.mj.z80.instructions;

import com.mj.exceptions.DeviceUnavailable;
import com.mj.exceptions.ROException;
import com.mj.exceptions.illegalAddressException;
import com.mj.z80.Z80CPU;
import com.mj.z80.Z80Instruction;

/** LD (nn),A ($32) - stores A to a direct 16-bit memory address. See
 * LoadAFromMemory's class comment - same A-only addressing form. Never
 * affects any flag. */
public final class StoreAToMemory implements Z80Instruction {
	public static final StoreAToMemory INSTANCE = new StoreAToMemory();

	private StoreAToMemory() {
	}

	@Override
	public void execute(Z80CPU cpu) throws illegalAddressException, DeviceUnavailable, ROException {
		int address = (cpu.fetchByte() & 0xff) | ((cpu.fetchByte() & 0xff) << 8);
		cpu.memoryBus.write(address, (byte) cpu.af.hi());
	}
}
