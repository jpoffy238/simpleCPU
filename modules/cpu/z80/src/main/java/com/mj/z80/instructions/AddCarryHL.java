package com.mj.z80.instructions;

import com.mj.z80.RegisterPair16;
import com.mj.z80.Z80CPU;
import com.mj.z80.Z80Instruction;

/** ADC HL,rr (ED-prefixed, $4A-$7A) - 16-bit add-with-carry into HL,
 * fully setting flags (unlike the unprefixed 16-bit ADD HL,rr/INC/DEC,
 * which don't - a different, simpler family not built yet). */
public final class AddCarryHL implements Z80Instruction {
	private final RegisterPair16 source;

	public AddCarryHL(RegisterPair16 source) {
		this.source = source;
	}

	@Override
	public void execute(Z80CPU cpu) {
		ArithmeticOperations.addSubHL(cpu, source.get16(cpu), false);
	}
}
