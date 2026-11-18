package com.mj.z80.instructions;

import com.mj.z80.Register8;
import com.mj.z80.Z80CPU;
import com.mj.z80.Z80Instruction;

/** RLC/RRC/RL/RR/SLA/SRA/SRL r - the register-operand tier of the CB
 * rotate/shift family (see RotateShiftTable). */
public final class RotateShiftRegister implements Z80Instruction {
	private final RotateShiftOperation operation;
	private final Register8 register;

	public RotateShiftRegister(RotateShiftOperation operation, Register8 register) {
		this.operation = operation;
		this.register = register;
	}

	@Override
	public void execute(Z80CPU cpu) {
		register.set(cpu, operation.apply(cpu, register.get(cpu)));
	}
}
