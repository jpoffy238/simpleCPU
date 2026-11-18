package com.mj.z80.instructions;

import com.mj.z80.Register8;
import com.mj.z80.Z80InstructionDecoder;

/** Populates the base table's whole LD r,r' family: every register-to-
 * register combination of the `01 ddd sss` opcode shape, ddd/sss each one
 * of Register8's seven values. $76 (ddd=sss=110, i.e. "LD (HL),(HL)") can
 * never come out of this loop at all - that code means (HL), which isn't
 * a Register8 value - which is exactly right: that slot is real hardware's
 * HALT opcode, not a register load, and belongs to a different
 * instruction family entirely. (HL)/(IX+d)/(IY+d) as either operand are
 * likewise out of scope here - a memory access, not a register, gets its
 * own instruction shape once that family is built. */
public final class LoadRegisterTable {
	private LoadRegisterTable() {
	}

	public static void populate(Z80InstructionDecoder decoder) {
		for (Register8 destination : Register8.values()) {
			for (Register8 source : Register8.values()) {
				int opcode = 0x40 | (destination.code() << 3) | source.code();
				decoder.setBase(opcode, new LoadRegister(destination, source));
			}
		}
	}
}
