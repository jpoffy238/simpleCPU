package com.mj.z80.instructions;

import com.mj.z80.Register8;
import com.mj.z80.Z80InstructionDecoder;

/** Populates the base table's LD r,n opcodes (`00 rrr 110`) for every
 * Register8 destination. $36 (rrr=110, i.e. (HL)'s code) is deliberately
 * not produced by this loop - that's LD (HL),n, a memory write rather than
 * a register load, and gets its own instruction shape once that family is
 * built, same reasoning as LoadRegisterTable skipping (HL) throughout. */
public final class LoadImmediateTable {
	private LoadImmediateTable() {
	}

	public static void populate(Z80InstructionDecoder decoder) {
		for (Register8 destination : Register8.values()) {
			int opcode = 0x06 | (destination.code() << 3);
			decoder.setBase(opcode, new LoadImmediate(destination));
		}
	}
}
