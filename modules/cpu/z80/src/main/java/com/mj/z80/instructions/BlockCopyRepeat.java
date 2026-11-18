package com.mj.z80.instructions;

import com.mj.exceptions.DeviceUnavailable;
import com.mj.exceptions.ROException;
import com.mj.exceptions.illegalAddressException;
import com.mj.z80.Z80CPU;
import com.mj.z80.Z80Instruction;

/** LDIR (direction +1, $B0) / LDDR (direction -1, $B8) - repeats
 * BlockOperations.copyStep() until BC reaches 0, as a single Java loop
 * (see BlockOperations' class comment for why). A do-while, not a
 * while - if BC is already 0 when this starts, real hardware treats
 * that as "a full 65536", not "nothing to do", since decrementing 0
 * wraps to 0xFFFF rather than stopping; this reproduces that by always
 * running at least one step before checking. */
public final class BlockCopyRepeat implements Z80Instruction {
	private final int direction;

	public BlockCopyRepeat(int direction) {
		this.direction = direction;
	}

	@Override
	public void execute(Z80CPU cpu) throws illegalAddressException, DeviceUnavailable, ROException {
		do {
			BlockOperations.copyStep(cpu, direction);
		} while (cpu.bc.get16() != 0);
	}
}
