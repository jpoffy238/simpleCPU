package com.mj.z80.instructions;

import com.mj.exceptions.DeviceUnavailable;
import com.mj.exceptions.illegalAddressException;
import com.mj.z80.Z80CPU;
import com.mj.z80.Z80Instruction;

/** RETN (ED $45) - pops the return address back into PC, same as RET,
 * but also restores IFF1 from IFF2 - the NMI entry sequence (see
 * Z80CPU.serviceInterrupts()) only ever clears IFF1, never IFF2, so this
 * is what lets a handler re-enable interrupts to whatever state they were
 * actually in before the NMI landed, without needing to know that state
 * itself. */
public final class ReturnFromNmi implements Z80Instruction {
	public static final ReturnFromNmi INSTANCE = new ReturnFromNmi();

	private ReturnFromNmi() {
	}

	@Override
	public void execute(Z80CPU cpu) throws illegalAddressException, DeviceUnavailable {
		cpu.pc = StackHelper.pop16(cpu);
		cpu.iff1 = cpu.iff2;
	}
}
