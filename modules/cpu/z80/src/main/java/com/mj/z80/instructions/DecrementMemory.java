package com.mj.z80.instructions;

import com.mj.exceptions.DeviceUnavailable;
import com.mj.exceptions.ROException;
import com.mj.exceptions.illegalAddressException;
import com.mj.z80.Z80CPU;
import com.mj.z80.Z80Instruction;

/** DEC (HL) ($35) - same flag rules as DecrementRegister, applied to the
 * byte at [HL]. Stateless, so one shared instance covers every call. */
public final class DecrementMemory implements Z80Instruction {
	public static final DecrementMemory INSTANCE = new DecrementMemory();

	private DecrementMemory() {
	}

	@Override
	public void execute(Z80CPU cpu) throws illegalAddressException, DeviceUnavailable, ROException {
		int address = cpu.hl.get16();
		int value = cpu.memoryBus.read(address) & 0xff;
		cpu.memoryBus.write(address, (byte) IncDecOperations.decrement(cpu, value));
	}
}
