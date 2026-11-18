package com.mj.z80.instructions;

import com.mj.exceptions.DeviceUnavailable;
import com.mj.exceptions.illegalAddressException;
import com.mj.z80.Z80CPU;
import com.mj.z80.Z80Instruction;

/** RET ($C9) - pops the return address back into PC. Stateless, so one
 * shared instance covers every call.
 *
 * RETI (ED $4D) and RETN (ED $45) are their own classes - see
 * ReturnFromInterrupt/ReturnFromNmi - since RETN also restores IFF1 from
 * IFF2, which this plain RET has no reason to touch. */
public final class Return implements Z80Instruction {
	public static final Return INSTANCE = new Return();

	private Return() {
	}

	@Override
	public void execute(Z80CPU cpu) throws illegalAddressException, DeviceUnavailable {
		cpu.pc = StackHelper.pop16(cpu);
	}
}
