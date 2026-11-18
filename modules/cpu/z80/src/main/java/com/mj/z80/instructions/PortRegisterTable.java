package com.mj.z80.instructions;

import com.mj.z80.Register8;
import com.mj.z80.Z80InstructionDecoder;

/** Populates the ED table's register-addressed port instructions: IN
 * r,(C) ($40 | r&lt;&lt;3) and OUT (C),r ($41 | r&lt;&lt;3), for all seven
 * Register8 values. The r=6 slots ($70/$71) are deliberately left
 * unregistered - $70 is a semi-documented "IN (C)" that discards its
 * result, and $71 doesn't exist at all; neither is worth building before
 * the more basic forms here see real use. */
public final class PortRegisterTable {
	private PortRegisterTable() {
	}

	public static void populate(Z80InstructionDecoder decoder) {
		for (Register8 r : Register8.values()) {
			decoder.setED(0x40 | (r.code() << 3), new InRegisterFromPort(r));
			decoder.setED(0x41 | (r.code() << 3), new OutPortFromRegister(r));
		}
	}
}
