package com.mj.z80.instructions;

import com.mj.exceptions.DeviceUnavailable;
import com.mj.exceptions.illegalAddressException;
import com.mj.z80.Z80CPU;
import com.mj.z80.Z80Instruction;

/** JR e ($18) - unconditional relative jump. The displacement is read as
 * a plain Java byte (signed, matching the real instruction's signed 8-bit
 * range) and added to PC - which, by the time execute() runs, already
 * points past this entire 2-byte instruction (fetchByte() already
 * advanced it reading the displacement itself), exactly matching real
 * hardware's own "relative to the next instruction" semantics with no
 * extra adjustment needed. Stateless, so one shared instance covers every
 * call. */
public final class JumpRelative implements Z80Instruction {
	public static final JumpRelative INSTANCE = new JumpRelative();

	private JumpRelative() {
	}

	@Override
	public void execute(Z80CPU cpu) throws illegalAddressException, DeviceUnavailable {
		byte displacement = cpu.fetchByte();
		cpu.pc = (cpu.pc + displacement) & 0xffff;
	}
}
