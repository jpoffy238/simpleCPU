package com.mj.z80.instructions;

import com.mj.exceptions.DeviceUnavailable;
import com.mj.exceptions.ROException;
import com.mj.exceptions.illegalAddressException;
import com.mj.z80.IndexRegister;
import com.mj.z80.Z80CPU;
import com.mj.z80.Z80Instruction;

/** LD (IX+d),n / LD (IY+d),n - writes a literal byte to (index register +
 * a signed displacement). Real byte order is prefix, opcode, displacement,
 * THEN the literal - displacement is read first here, matching that
 * order, unlike DD-CB's quirk where the decoder itself has to read the
 * displacement before it even knows which instruction this is (see
 * Z80InstructionDecoder) - this opcode is already fully identified by the
 * time execute() runs, so both bytes are this instruction's own problem to
 * read, in the real order they appear. Never affects any flag. */
public final class StoreImmediateToIndexed implements Z80Instruction {
	private final IndexRegister index;

	public StoreImmediateToIndexed(IndexRegister index) {
		this.index = index;
	}

	@Override
	public void execute(Z80CPU cpu) throws illegalAddressException, DeviceUnavailable, ROException {
		byte displacement = cpu.fetchByte();
		byte value = cpu.fetchByte();
		int address = (index.get(cpu).get16() + displacement) & 0xffff;
		cpu.memoryBus.write(address, value);
	}
}
