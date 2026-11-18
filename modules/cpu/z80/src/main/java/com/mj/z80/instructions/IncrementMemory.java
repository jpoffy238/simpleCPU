package com.mj.z80.instructions;

import com.mj.exceptions.DeviceUnavailable;
import com.mj.exceptions.ROException;
import com.mj.exceptions.illegalAddressException;
import com.mj.z80.Z80CPU;
import com.mj.z80.Z80Instruction;

/** INC (HL) ($34) - same flag rules as IncrementRegister, applied to the
 * byte at [HL] instead of a register. Stateless, so one shared instance
 * covers every call. */
public final class IncrementMemory implements Z80Instruction {
	public static final IncrementMemory INSTANCE = new IncrementMemory();

	private IncrementMemory() {
	}

	@Override
	public void execute(Z80CPU cpu) throws illegalAddressException, DeviceUnavailable, ROException {
		int address = cpu.hl.get16();
		int value = cpu.memoryBus.read(address) & 0xff;
		cpu.memoryBus.write(address, (byte) IncDecOperations.increment(cpu, value));
	}
}
