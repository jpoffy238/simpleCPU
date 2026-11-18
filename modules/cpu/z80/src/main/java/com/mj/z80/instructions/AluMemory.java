package com.mj.z80.instructions;

import com.mj.exceptions.DeviceUnavailable;
import com.mj.exceptions.illegalAddressException;
import com.mj.z80.Z80CPU;
import com.mj.z80.Z80Instruction;

/** ADD A,(HL) / ADC A,(HL) / SUB (HL) / SBC A,(HL) / AND (HL) / XOR (HL) /
 * OR (HL) / CP (HL) - the plain-HL memory tier of the 8-bit ALU family
 * (same `10 ooo rrr` shape as AluRegister, with rrr=110 meaning (HL)
 * rather than a register - the same split LoadFromMemory/LoadRegister
 * already drew). */
public final class AluMemory implements Z80Instruction {
	private final AluOperation operation;

	public AluMemory(AluOperation operation) {
		this.operation = operation;
	}

	@Override
	public void execute(Z80CPU cpu) throws illegalAddressException, DeviceUnavailable {
		int operand = cpu.memoryBus.read(cpu.hl.get16()) & 0xff;
		operation.apply(cpu, operand);
	}
}
