package com.mj.z80.instructions;

import com.mj.z80.Register8;
import com.mj.z80.Z80CPU;
import com.mj.z80.Z80Instruction;

/** BIT b,r (`01 bbb rrr`) - the register-operand tier of the CB bit-test
 * family (see BitTable). Never modifies its operand - only sets flags. */
public final class BitTestRegister implements Z80Instruction {
	private final int bitIndex;
	private final Register8 register;

	public BitTestRegister(int bitIndex, Register8 register) {
		this.bitIndex = bitIndex;
		this.register = register;
	}

	@Override
	public void execute(Z80CPU cpu) {
		BitOperations.bit(cpu, bitIndex, register.get(cpu));
	}
}
