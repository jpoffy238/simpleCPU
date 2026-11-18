package com.mj.z80.instructions;

import com.mj.exceptions.DeviceUnavailable;
import com.mj.exceptions.illegalAddressException;
import com.mj.z80.Condition;
import com.mj.z80.Z80CPU;
import com.mj.z80.Z80Instruction;

/** RET cc - conditional return. Unlike JP cc/CALL cc, there are no
 * "address bytes" to unconditionally consume here - a failed RET cc
 * leaves the stack (and SP) completely untouched; nothing at all is read
 * from it unless the condition holds. */
public final class ReturnConditional implements Z80Instruction {
	private final Condition condition;

	public ReturnConditional(Condition condition) {
		this.condition = condition;
	}

	@Override
	public void execute(Z80CPU cpu) throws illegalAddressException, DeviceUnavailable {
		if (condition.isMet(cpu)) {
			cpu.pc = StackHelper.pop16(cpu);
		}
	}
}
