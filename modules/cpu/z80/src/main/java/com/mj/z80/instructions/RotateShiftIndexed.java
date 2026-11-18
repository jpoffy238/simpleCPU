package com.mj.z80.instructions;

import com.mj.exceptions.DeviceUnavailable;
import com.mj.exceptions.ROException;
import com.mj.exceptions.illegalAddressException;
import com.mj.z80.IndexRegister;
import com.mj.z80.Z80CPU;
import com.mj.z80.Z80Instruction;

/** RLC/RRC/RL/RR/SLA/SRA/SRL (IX+d)/(IY+d) - the DD-CB/FD-CB indexed tier
 * of the rotate/shift family. Unlike every other indexed instruction,
 * the displacement here is NOT read via cpu.fetchByte() during
 * execute() - the decoder itself already had to read it before this
 * instruction could even be identified (DD-CB/FD-CB's own byte order:
 * prefix, $CB, displacement, THEN the final opcode - see
 * Z80InstructionDecoder), and stashed it in cpu.pendingDisplacement for
 * exactly this moment. Every other rrr value under DD-CB/FD-CB besides
 * this one ((HL)'s own slot, $x6) is undocumented IXH/IXL-touching
 * territory and isn't registered at all - see RotateShiftTable. */
public final class RotateShiftIndexed implements Z80Instruction {
	private final RotateShiftOperation operation;
	private final IndexRegister index;

	public RotateShiftIndexed(RotateShiftOperation operation, IndexRegister index) {
		this.operation = operation;
		this.index = index;
	}

	@Override
	public void execute(Z80CPU cpu) throws illegalAddressException, DeviceUnavailable, ROException {
		int address = (index.get(cpu).get16() + cpu.pendingDisplacement) & 0xffff;
		int value = cpu.memoryBus.read(address) & 0xff;
		cpu.memoryBus.write(address, (byte) operation.apply(cpu, value));
	}
}
