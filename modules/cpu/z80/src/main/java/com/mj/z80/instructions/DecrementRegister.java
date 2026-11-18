package com.mj.z80.instructions;

import com.mj.z80.Register8;
import com.mj.z80.Z80CPU;
import com.mj.z80.Z80Instruction;

/** DEC r (`00 rrr 101`, $05-$3D) - see IncDecOperations for the flag
 * rules (notably: C is left untouched). */
public final class DecrementRegister implements Z80Instruction {
	private final Register8 register;

	public DecrementRegister(Register8 register) {
		this.register = register;
	}

	@Override
	public void execute(Z80CPU cpu) {
		register.set(cpu, IncDecOperations.decrement(cpu, register.get(cpu)));
	}
}
