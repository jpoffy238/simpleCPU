package com.mj.z80.instructions;

import com.mj.z80.Register8;
import com.mj.z80.Z80InstructionDecoder;

/** Populates the base table's LD r,(HL) (`01 rrr 110`, $46-$7E), LD (HL),r
 * (`01 110 rrr`, $70-$77), and LD (HL),n ($36) opcodes - every plain-HL
 * memory-indirect form. $76 (both "roles" being (HL) at once) is real
 * hardware's HALT opcode, not a register load - Register8 has no (HL)
 * value, so neither loop below can ever produce it, same reasoning as
 * LoadRegisterTable skipping it. */
public final class LoadMemoryTable {
	private LoadMemoryTable() {
	}

	public static void populate(Z80InstructionDecoder decoder) {
		for (Register8 r : Register8.values()) {
			decoder.setBase(0x40 | (r.code() << 3) | 0x06, new LoadFromMemory(r));
			decoder.setBase(0x70 | r.code(), new StoreToMemory(r));
		}
		decoder.setBase(0x36, StoreImmediateToMemory.INSTANCE);
	}
}
