package com.mj.z80.instructions;

import com.mj.exceptions.DeviceUnavailable;
import com.mj.exceptions.illegalAddressException;
import com.mj.z80.Z80CPU;
import com.mj.z80.Z80Instruction;

/** JP nn ($C3) - unconditional absolute jump. Stateless, so one shared
 * instance covers every call. */
public final class Jump implements Z80Instruction {
	public static final Jump INSTANCE = new Jump();

	private Jump() {
	}

	@Override
	public void execute(Z80CPU cpu) throws illegalAddressException, DeviceUnavailable {
		int low = cpu.fetchByte() & 0xff;
		int high = cpu.fetchByte() & 0xff;
		cpu.pc = low | (high << 8);
	}
}
