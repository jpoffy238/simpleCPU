package com.mj.z80.instructions;

import com.mj.z80.Z80InstructionDecoder;

/** Populates the base table's plain-HL ALU opcodes (`10 ooo 110`) - the
 * exact slots AluRegisterTable's own loop can never produce (Register8
 * has no (HL) value), filled in here the same way LoadMemoryTable fills
 * the gaps LoadRegisterTable leaves. */
public final class AluMemoryTable {
	private AluMemoryTable() {
	}

	public static void populate(Z80InstructionDecoder decoder) {
		for (AluOperation op : AluOperation.values()) {
			int opcode = 0x80 | (op.code() << 3) | 0x06;
			decoder.setBase(opcode, new AluMemory(op));
		}
	}
}
