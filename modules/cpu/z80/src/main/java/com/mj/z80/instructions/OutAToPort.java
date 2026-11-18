package com.mj.z80.instructions;

import com.mj.exceptions.DeviceUnavailable;
import com.mj.exceptions.ROException;
import com.mj.exceptions.illegalAddressException;
import com.mj.z80.Z80CPU;
import com.mj.z80.Z80Instruction;

/** OUT (n),A ($D3, unprefixed) - writes A to the port (A&lt;&lt;8)|n, mirroring
 * IN A,(n)'s own effective port address. Affects no flags. */
public final class OutAToPort implements Z80Instruction {
	public static final OutAToPort INSTANCE = new OutAToPort();

	private OutAToPort() {
	}

	@Override
	public void execute(Z80CPU cpu) throws illegalAddressException, DeviceUnavailable, ROException {
		int n = cpu.fetchByte() & 0xff;
		int a = cpu.af.hi();
		int port = (a << 8) | n;
		cpu.portBus.write(port, (byte) a);
	}
}
