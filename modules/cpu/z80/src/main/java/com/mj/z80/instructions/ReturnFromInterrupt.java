package com.mj.z80.instructions;

import com.mj.exceptions.DeviceUnavailable;
import com.mj.exceptions.illegalAddressException;
import com.mj.z80.Z80CPU;
import com.mj.z80.Z80Instruction;

/** RETI (ED $4D) - pops the return address back into PC. Functionally
 * identical to plain RET here; real hardware's only difference is a
 * signal to daisy-chained interrupt-priority hardware that service has
 * ended, which doesn't exist in this emulator. Unlike RETN, it does not
 * touch IFF1/IFF2 - it ends a maskable interrupt, which never auto-clears
 * IFF2 the way NMI entry clears IFF1. */
public final class ReturnFromInterrupt implements Z80Instruction {
	public static final ReturnFromInterrupt INSTANCE = new ReturnFromInterrupt();

	private ReturnFromInterrupt() {
	}

	@Override
	public void execute(Z80CPU cpu) throws illegalAddressException, DeviceUnavailable {
		cpu.pc = StackHelper.pop16(cpu);
	}
}
