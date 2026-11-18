package com.mj.z80.instructions;

import com.mj.exceptions.DeviceUnavailable;
import com.mj.exceptions.illegalAddressException;
import com.mj.z80.Condition;
import com.mj.z80.Z80CPU;
import com.mj.z80.Z80Instruction;

/** JR cc,e - conditional relative jump (only NZ/Z/NC/C exist - see
 * JumpRelativeTable). The displacement is always consumed, whether or not
 * the condition holds, same reasoning as JumpConditional. */
public final class JumpRelativeConditional implements Z80Instruction {
	private final Condition condition;

	public JumpRelativeConditional(Condition condition) {
		this.condition = condition;
	}

	@Override
	public void execute(Z80CPU cpu) throws illegalAddressException, DeviceUnavailable {
		byte displacement = cpu.fetchByte();
		if (condition.isMet(cpu)) {
			cpu.pc = (cpu.pc + displacement) & 0xffff;
		}
	}
}
