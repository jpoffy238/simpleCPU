package com.mj.z80.instructions;

import com.mj.z80.Register8;
import com.mj.z80.Z80InstructionDecoder;

/** Populates the base table's LD r,r' opcodes (`01 ddd sss`, $40-$7F minus
 * the $x6/$xE columns - those are (HL), a different instruction shape).
 * Only LD A,r is populated so far ($78-$7D, $7F - $7E is LD A,(HL), out of
 * scope here) - one destination register at a time, same incremental
 * approach as every other instruction family in this project; more rows
 * join this same method as the rest of the family gets built. */
public final class LoadRegisterTable {
	private LoadRegisterTable() {
	}

	public static void populate(Z80InstructionDecoder decoder) {
		decoder.setBase(0x78, new LoadRegister(Register8.A, Register8.B));
		decoder.setBase(0x79, new LoadRegister(Register8.A, Register8.C));
		decoder.setBase(0x7A, new LoadRegister(Register8.A, Register8.D));
		decoder.setBase(0x7B, new LoadRegister(Register8.A, Register8.E));
		decoder.setBase(0x7C, new LoadRegister(Register8.A, Register8.H));
		decoder.setBase(0x7D, new LoadRegister(Register8.A, Register8.L));
		decoder.setBase(0x7F, new LoadRegister(Register8.A, Register8.A));
	}
}
