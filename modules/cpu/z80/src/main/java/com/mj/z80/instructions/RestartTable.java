package com.mj.z80.instructions;

import com.mj.z80.Z80InstructionDecoder;

/** Populates RST p (`11 ppp 111`, $C7-$FF) - p's target address is p*8. */
public final class RestartTable {
	private RestartTable() {
	}

	public static void populate(Z80InstructionDecoder decoder) {
		for (int p = 0; p < 8; p++) {
			decoder.setBase(0xC7 | (p << 3), new Restart(p * 8));
		}
	}
}
