package com.mj.z80.instructions;

import com.mj.z80.Register8;
import com.mj.z80.Z80InstructionDecoder;

/** Populates the base table's INC r/DEC r opcodes - every Register8
 * straight from the formula, same style as every other register-coded
 * family. */
public final class IncDecRegisterTable {
	private IncDecRegisterTable() {
	}

	public static void populate(Z80InstructionDecoder decoder) {
		for (Register8 r : Register8.values()) {
			decoder.setBase(0x04 | (r.code() << 3), new IncrementRegister(r));
			decoder.setBase(0x05 | (r.code() << 3), new DecrementRegister(r));
		}
	}
}
