package com.mj.z80.instructions;

import com.mj.z80.Register8;
import com.mj.z80.Z80CPU;
import com.mj.z80.Z80Instruction;

/** RES b,r (`10 bbb rrr`) / SET b,r (`11 bbb rrr`) - clears or sets one
 * bit of a register and writes it back. Both share this one class,
 * parameterized by setBit (false=RES, true=SET) - identical shape,
 * differing only in which BitOperations method is called. Affects no
 * flag at all. */
public final class ModifyBitRegister implements Z80Instruction {
	private final int bitIndex;
	private final boolean setBit;
	private final Register8 register;

	public ModifyBitRegister(int bitIndex, boolean setBit, Register8 register) {
		this.bitIndex = bitIndex;
		this.setBit = setBit;
		this.register = register;
	}

	@Override
	public void execute(Z80CPU cpu) {
		int value = register.get(cpu);
		int result = setBit ? BitOperations.set(bitIndex, value) : BitOperations.res(bitIndex, value);
		register.set(cpu, result);
	}
}
