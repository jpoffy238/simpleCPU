package com.mj.z80.instructions;

import com.mj.z80.RegisterPair16;
import com.mj.z80.Z80CPU;
import com.mj.z80.Z80Instruction;

/** SBC HL,rr (ED-prefixed, $42-$72) - 16-bit subtract-with-carry from
 * HL, fully setting flags, same reasoning as AddCarryHL. */
public final class SubtractCarryHL implements Z80Instruction {
	private final RegisterPair16 source;

	public SubtractCarryHL(RegisterPair16 source) {
		this.source = source;
	}

	@Override
	public void execute(Z80CPU cpu) {
		ArithmeticOperations.addSubHL(cpu, source.get16(cpu), true);
	}
}
