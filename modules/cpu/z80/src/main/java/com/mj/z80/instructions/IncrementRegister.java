package com.mj.z80.instructions;

import com.mj.z80.Register8;
import com.mj.z80.Z80CPU;
import com.mj.z80.Z80Instruction;

/** INC r (`00 rrr 100`, $04-$3C) - see IncDecOperations for the flag
 * rules (notably: C is left untouched, unlike every ALU operation). */
public final class IncrementRegister implements Z80Instruction {
	private final Register8 register;

	public IncrementRegister(Register8 register) {
		this.register = register;
	}

	@Override
	public void execute(Z80CPU cpu) {
		register.set(cpu, IncDecOperations.increment(cpu, register.get(cpu)));
	}
}
