package com.mj.z80.instructions;

import com.mj.exceptions.DeviceUnavailable;
import com.mj.exceptions.illegalAddressException;
import com.mj.z80.IndexRegister;
import com.mj.z80.Z80CPU;
import com.mj.z80.Z80Instruction;

/** LD IX,nn / LD IY,nn - the same $21 opcode as LD HL,nn, just under the
 * DD/FD prefix. Kept as its own small class (rather than reusing
 * LoadRegisterPairImmediate) since IX/IY are selected via IndexRegister,
 * not RegisterPair16 - the same split already used for LD r,(IX+d) etc.
 * Never affects any flag. */
public final class LoadIndexImmediate implements Z80Instruction {
	private final IndexRegister index;

	public LoadIndexImmediate(IndexRegister index) {
		this.index = index;
	}

	@Override
	public void execute(Z80CPU cpu) throws illegalAddressException, DeviceUnavailable {
		int low = cpu.fetchByte() & 0xff;
		int high = cpu.fetchByte() & 0xff;
		index.get(cpu).set16(low | (high << 8));
	}
}
