package com.mj.z80.instructions;

import com.mj.exceptions.DeviceUnavailable;
import com.mj.exceptions.illegalAddressException;
import com.mj.z80.Z80CPU;
import com.mj.z80.Z80Instruction;

/** DJNZ e ($10) - decrements B, then jumps relative if the result isn't
 * zero. The classic Z80 "loop N times" idiom (load B with a count, DJNZ
 * back to the loop's start). The displacement is always read, same as
 * every conditional relative jump, but the decrement itself is
 * unconditional - B always goes down by one, whether or not the jump is
 * taken. Unlike DEC B, this decrement never affects any flag - a real,
 * documented quirk specific to DJNZ, not an oversight. Stateless, so one
 * shared instance covers every call. */
public final class DecrementAndJumpIfNotZero implements Z80Instruction {
	public static final DecrementAndJumpIfNotZero INSTANCE = new DecrementAndJumpIfNotZero();

	private DecrementAndJumpIfNotZero() {
	}

	@Override
	public void execute(Z80CPU cpu) throws illegalAddressException, DeviceUnavailable {
		byte displacement = cpu.fetchByte();
		int b = (cpu.bc.hi() - 1) & 0xff;
		cpu.bc.setHi(b);
		if (b != 0) {
			cpu.pc = (cpu.pc + displacement) & 0xffff;
		}
	}
}
