package com.mj.z80.instructions;

import com.mj.z80.Z80InstructionDecoder;

/** Populates the base table's two simple port instructions: IN A,(n)
 * ($DB) and OUT (n),A ($D3). The ED-prefixed register forms (IN r,(C) /
 * OUT (C),r) live in PortRegisterTable. */
public final class PortTable {
	private PortTable() {
	}

	public static void populate(Z80InstructionDecoder decoder) {
		decoder.setBase(0xDB, InAFromPort.INSTANCE);
		decoder.setBase(0xD3, OutAToPort.INSTANCE);
	}
}
