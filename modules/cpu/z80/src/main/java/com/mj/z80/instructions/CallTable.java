package com.mj.z80.instructions;

import com.mj.z80.Condition;
import com.mj.z80.Z80InstructionDecoder;

/** Populates CALL nn ($CD) and CALL cc,nn (`11 ccc 100`, $C4-$FC). */
public final class CallTable {
	private CallTable() {
	}

	public static void populate(Z80InstructionDecoder decoder) {
		decoder.setBase(0xCD, Call.INSTANCE);
		for (Condition cc : Condition.values()) {
			decoder.setBase(0xC4 | (cc.code() << 3), new CallConditional(cc));
		}
	}
}
