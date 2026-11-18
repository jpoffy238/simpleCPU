package com.mj.z80.instructions;

import com.mj.exceptions.DeviceUnavailable;
import com.mj.exceptions.illegalAddressException;
import com.mj.z80.Z80CPU;
import com.mj.z80.Z80Instruction;

/** IN A,(n) ($DB, unprefixed) - reads an 8-bit immediate port number n,
 * but real hardware also puts A on the upper half of the 16-bit port
 * address bus, so the effective port is (A&lt;&lt;8)|n, not just n. Affects
 * no flags at all - a real, documented asymmetry with the ED-prefixed
 * IN r,(C) family, which does set S/Z/P-V. */
public final class InAFromPort implements Z80Instruction {
	public static final InAFromPort INSTANCE = new InAFromPort();

	private InAFromPort() {
	}

	@Override
	public void execute(Z80CPU cpu) throws illegalAddressException, DeviceUnavailable {
		int n = cpu.fetchByte() & 0xff;
		int port = (cpu.af.hi() << 8) | n;
		int value = cpu.portBus.read(port) & 0xff;
		cpu.af.setHi(value);
	}
}
