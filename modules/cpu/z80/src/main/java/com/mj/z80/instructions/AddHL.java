package com.mj.z80.instructions;

import com.mj.z80.RegisterPair16;
import com.mj.z80.Z80CPU;
import com.mj.z80.Z80Instruction;

/** ADD HL,rr (unprefixed, $09/$19/$29/$39) - see
 * ArithmeticOperations.addHL() for the flag shape, which is distinct from
 * both 16-bit INC/DEC and the ED-prefixed ADC HL,rr/SBC HL,rr. */
public final class AddHL implements Z80Instruction {
	private final RegisterPair16 source;

	public AddHL(RegisterPair16 source) {
		this.source = source;
	}

	@Override
	public void execute(Z80CPU cpu) {
		ArithmeticOperations.addHL(cpu, source.get16(cpu));
	}
}
