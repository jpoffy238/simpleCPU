package com.mj.z80.instructions;

import com.mj.exceptions.DeviceUnavailable;
import com.mj.exceptions.illegalAddressException;
import com.mj.z80.IndexRegister;
import com.mj.z80.Z80CPU;
import com.mj.z80.Z80Instruction;

/** ADD A,(IX+d) / .../ CP (IY+d) - the indexed-memory tier of the 8-bit
 * ALU family. Same displacement-read-during-execute convention, and the
 * same no-H/L-entanglement property, as LoadFromIndexed - see its class
 * comment. */
public final class AluIndexed implements Z80Instruction {
	private final AluOperation operation;
	private final IndexRegister index;

	public AluIndexed(AluOperation operation, IndexRegister index) {
		this.operation = operation;
		this.index = index;
	}

	@Override
	public void execute(Z80CPU cpu) throws illegalAddressException, DeviceUnavailable {
		byte displacement = cpu.fetchByte();
		int address = (index.get(cpu).get16() + displacement) & 0xffff;
		int operand = cpu.memoryBus.read(address) & 0xff;
		operation.apply(cpu, operand);
	}
}
