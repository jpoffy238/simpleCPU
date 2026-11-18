package com.mj.z80.instructions;

import com.mj.exceptions.DeviceUnavailable;
import com.mj.exceptions.ROException;
import com.mj.exceptions.illegalAddressException;
import com.mj.z80.Z80CPU;
import com.mj.z80.Z80Instruction;

/** CALL nn ($CD) - pushes the return address (PC, which by the time this
 * runs already points past this whole 3-byte instruction - both address
 * bytes were already consumed via fetchByte()) and jumps to the target.
 * Stateless, so one shared instance covers every call. */
public final class Call implements Z80Instruction {
	public static final Call INSTANCE = new Call();

	private Call() {
	}

	@Override
	public void execute(Z80CPU cpu) throws illegalAddressException, DeviceUnavailable, ROException {
		int low = cpu.fetchByte() & 0xff;
		int high = cpu.fetchByte() & 0xff;
		int target = low | (high << 8);
		StackHelper.push16(cpu, cpu.pc);
		cpu.pc = target;
	}
}
