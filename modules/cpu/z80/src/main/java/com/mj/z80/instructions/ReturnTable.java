package com.mj.z80.instructions;

import com.mj.z80.Condition;
import com.mj.z80.Z80InstructionDecoder;

/** Populates RET ($C9) and RET cc (`11 ccc 000`, $C0-$F8). */
public final class ReturnTable {
	private ReturnTable() {
	}

	public static void populate(Z80InstructionDecoder decoder) {
		decoder.setBase(0xC9, Return.INSTANCE);
		for (Condition cc : Condition.values()) {
			decoder.setBase(0xC0 | (cc.code() << 3), new ReturnConditional(cc));
		}
	}
}
