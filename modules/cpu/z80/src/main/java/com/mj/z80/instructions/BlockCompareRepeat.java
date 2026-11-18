package com.mj.z80.instructions;

import com.mj.exceptions.DeviceUnavailable;
import com.mj.exceptions.illegalAddressException;
import com.mj.z80.Z80CPU;
import com.mj.z80.Z80Instruction;

/** CPIR (direction +1, $B1) / CPDR (direction -1, $B9) - repeats
 * BlockOperations.compareStep() until either BC reaches 0 or a match is
 * found (A equals the compared byte), whichever comes first - unlike
 * LDIR/LDDR's own repeat condition, which only ever checks BC. Same
 * do-while/single-Java-loop reasoning as BlockCopyRepeat. */
public final class BlockCompareRepeat implements Z80Instruction {
	private final int direction;

	public BlockCompareRepeat(int direction) {
		this.direction = direction;
	}

	@Override
	public void execute(Z80CPU cpu) throws illegalAddressException, DeviceUnavailable {
		boolean matched;
		do {
			matched = BlockOperations.compareStep(cpu, direction);
		} while (!matched && cpu.bc.get16() != 0);
	}
}
