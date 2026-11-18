package com.mj.z80.instructions;

import com.mj.exceptions.DeviceUnavailable;
import com.mj.exceptions.ROException;
import com.mj.exceptions.illegalAddressException;
import com.mj.z80.Condition;
import com.mj.z80.Z80CPU;
import com.mj.z80.Z80Instruction;

/** CALL cc,nn - conditional call. The address bytes are always consumed,
 * same reasoning as JumpConditional; the push itself only happens if the
 * condition holds - a failed CALL cc must leave the stack completely
 * untouched, not just skip the jump. */
public final class CallConditional implements Z80Instruction {
	private final Condition condition;

	public CallConditional(Condition condition) {
		this.condition = condition;
	}

	@Override
	public void execute(Z80CPU cpu) throws illegalAddressException, DeviceUnavailable, ROException {
		int low = cpu.fetchByte() & 0xff;
		int high = cpu.fetchByte() & 0xff;
		if (condition.isMet(cpu)) {
			StackHelper.push16(cpu, cpu.pc);
			cpu.pc = low | (high << 8);
		}
	}
}
