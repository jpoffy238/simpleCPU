package com.mj.z80.instructions;

import com.mj.z80.Register8;
import com.mj.z80.Z80CPU;
import com.mj.z80.Z80Instruction;

/** ADD A,r / ADC A,r / SUB r / SBC A,r / AND r / XOR r / OR r / CP r - the
 * register-operand tier of the 8-bit ALU family (`10 ooo rrr`). Immediate
 * ($C6-$FE), (HL), and (IX+d)/(IY+d) tiers are the same eight operations
 * against a different operand source - they get their own classes once
 * those tiers are built, same incremental approach as the LD family
 * (register tier first, then immediate, then memory, then indexed). */
public final class AluRegister implements Z80Instruction {
	private final AluOperation operation;
	private final Register8 source;

	public AluRegister(AluOperation operation, Register8 source) {
		this.operation = operation;
		this.source = source;
	}

	@Override
	public void execute(Z80CPU cpu) {
		operation.apply(cpu, source.get(cpu));
	}
}
