package com.mj.z80.instructions;

import com.mj.exceptions.DeviceUnavailable;
import com.mj.exceptions.ROException;
import com.mj.exceptions.illegalAddressException;
import com.mj.z80.Z80CPU;
import com.mj.z80.Z80Instruction;

/** RLC/RRC/RL/RR/SLA/SRA/SRL (HL) - the plain-HL memory tier of the CB
 * rotate/shift family (`00 ooo 110`). */
public final class RotateShiftMemory implements Z80Instruction {
	private final RotateShiftOperation operation;

	public RotateShiftMemory(RotateShiftOperation operation) {
		this.operation = operation;
	}

	@Override
	public void execute(Z80CPU cpu) throws illegalAddressException, DeviceUnavailable, ROException {
		int address = cpu.hl.get16();
		int value = cpu.memoryBus.read(address) & 0xff;
		cpu.memoryBus.write(address, (byte) operation.apply(cpu, value));
	}
}
