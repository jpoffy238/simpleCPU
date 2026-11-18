package com.mj.z80.instructions;

import com.mj.exceptions.DeviceUnavailable;
import com.mj.exceptions.illegalAddressException;
import com.mj.z80.Condition;
import com.mj.z80.Z80CPU;
import com.mj.z80.Z80Instruction;

/** JP cc,nn - conditional absolute jump. The address bytes are always
 * consumed, whether or not the condition holds - real hardware's own
 * instruction length never varies with a condition's outcome, only
 * whether PC actually gets overwritten does. */
public final class JumpConditional implements Z80Instruction {
	private final Condition condition;

	public JumpConditional(Condition condition) {
		this.condition = condition;
	}

	@Override
	public void execute(Z80CPU cpu) throws illegalAddressException, DeviceUnavailable {
		int low = cpu.fetchByte() & 0xff;
		int high = cpu.fetchByte() & 0xff;
		if (condition.isMet(cpu)) {
			cpu.pc = low | (high << 8);
		}
	}
}
