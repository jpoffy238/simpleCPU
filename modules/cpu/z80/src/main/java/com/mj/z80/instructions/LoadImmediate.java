package com.mj.z80.instructions;

import com.mj.exceptions.DeviceUnavailable;
import com.mj.exceptions.illegalAddressException;
import com.mj.z80.Register8;
import com.mj.z80.Z80CPU;
import com.mj.z80.Z80Instruction;

/** LD r,n - loads a literal byte, following the opcode in the instruction
 * stream, into one 8-bit register. The immediate byte is read here, during
 * execute() via cpu.fetchByte(), not pre-consumed by the decoder - same
 * convention as every ordinary (non-DD-CB/FD-CB) operand byte. Never
 * affects any flag, same as LD r,r' (see LoadRegister). */
public final class LoadImmediate implements Z80Instruction {
	private final Register8 destination;

	public LoadImmediate(Register8 destination) {
		this.destination = destination;
	}

	@Override
	public void execute(Z80CPU cpu) throws illegalAddressException, DeviceUnavailable {
		destination.set(cpu, cpu.fetchByte() & 0xff);
	}
}
