package com.mj.z80.instructions;

import com.mj.exceptions.DeviceUnavailable;
import com.mj.exceptions.ROException;
import com.mj.exceptions.illegalAddressException;
import com.mj.z80.Register8;
import com.mj.z80.Z80CPU;
import com.mj.z80.Z80Instruction;

/** OUT (C),r (ED $41 | r&lt;&lt;3) - writes r to the port addressed by the
 * full BC register. Affects no flags, same as every output instruction. */
public final class OutPortFromRegister implements Z80Instruction {
	private final Register8 source;

	public OutPortFromRegister(Register8 source) {
		this.source = source;
	}

	@Override
	public void execute(Z80CPU cpu) throws illegalAddressException, DeviceUnavailable, ROException {
		int port = cpu.bc.get16();
		cpu.portBus.write(port, (byte) source.get(cpu));
	}
}
