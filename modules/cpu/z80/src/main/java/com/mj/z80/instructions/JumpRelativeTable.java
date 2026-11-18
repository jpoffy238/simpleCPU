package com.mj.z80.instructions;

import com.mj.z80.Condition;
import com.mj.z80.Z80InstructionDecoder;

/** Populates DJNZ ($10), JR e ($18), and JR cc,e (`00 1cc 000`, $20-$38 -
 * only NZ/Z/NC/C, named explicitly rather than taking the first four of
 * Condition.values() - clearer about why only four exist (JR genuinely
 * can't test PO/PE/P/M on real hardware) than relying on that enum's
 * declaration order never changing). */
public final class JumpRelativeTable {
	private JumpRelativeTable() {
	}

	public static void populate(Z80InstructionDecoder decoder) {
		decoder.setBase(0x10, DecrementAndJumpIfNotZero.INSTANCE);
		decoder.setBase(0x18, JumpRelative.INSTANCE);
		for (Condition cc : new Condition[] { Condition.NZ, Condition.Z, Condition.NC, Condition.C }) {
			decoder.setBase(0x20 | (cc.code() << 3), new JumpRelativeConditional(cc));
		}
	}
}
