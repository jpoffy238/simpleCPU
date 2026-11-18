package com.mj.z80.instructions;

import com.mj.exceptions.DeviceUnavailable;
import com.mj.exceptions.illegalAddressException;
import com.mj.z80.Z80CPU;
import com.mj.z80.Z80Instruction;

/** ADD A,n / ADC A,n / SUB n / SBC A,n / AND n / XOR n / OR n / CP n
 * (`11 ooo 110`, $C6-$FE) - the immediate-operand tier of the 8-bit ALU
 * family. The literal byte is read here, during execute(), same
 * convention as every other ordinary operand byte - not pre-consumed by
 * the decoder. */
public final class AluImmediate implements Z80Instruction {
	private final AluOperation operation;

	public AluImmediate(AluOperation operation) {
		this.operation = operation;
	}

	@Override
	public void execute(Z80CPU cpu) throws illegalAddressException, DeviceUnavailable {
		int operand = cpu.fetchByte() & 0xff;
		operation.apply(cpu, operand);
	}
}
