package com.mj.z80.instructions;

import com.mj.z80.Z80InstructionDecoder;

/** Populates the base table's immediate-operand ALU opcodes (`11 ooo 110`,
 * $C6-$FE) - every AluOperation straight from the formula. */
public final class AluImmediateTable {
	private AluImmediateTable() {
	}

	public static void populate(Z80InstructionDecoder decoder) {
		for (AluOperation op : AluOperation.values()) {
			int opcode = 0xC6 | (op.code() << 3);
			decoder.setBase(opcode, new AluImmediate(op));
		}
	}
}
