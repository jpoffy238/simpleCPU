package com.mj.z80.instructions;

import com.mj.z80.Register8;
import com.mj.z80.Z80InstructionDecoder;

/** Populates the base table's register-operand ALU opcodes (`10 ooo rrr`,
 * $80-$BF) - every AluOperation against every Register8, straight from
 * the formula, same style as every other family's register-coded loop. */
public final class AluRegisterTable {
	private AluRegisterTable() {
	}

	public static void populate(Z80InstructionDecoder decoder) {
		for (AluOperation op : AluOperation.values()) {
			for (Register8 r : Register8.values()) {
				int opcode = 0x80 | (op.code() << 3) | r.code();
				decoder.setBase(opcode, new AluRegister(op, r));
			}
		}
	}
}
