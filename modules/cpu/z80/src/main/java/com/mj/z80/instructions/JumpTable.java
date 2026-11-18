package com.mj.z80.instructions;

import com.mj.z80.Condition;
import com.mj.z80.IndexRegister;
import com.mj.z80.Z80InstructionDecoder;

/** Populates JP nn ($C3), JP cc,nn (`11 ccc 010`, $C2-$FA), and JP (HL)/
 * JP (IX)/JP (IY) ($E9 unprefixed/DD/FD). */
public final class JumpTable {
	private JumpTable() {
	}

	public static void populate(Z80InstructionDecoder decoder) {
		decoder.setBase(0xC3, Jump.INSTANCE);
		for (Condition cc : Condition.values()) {
			decoder.setBase(0xC2 | (cc.code() << 3), new JumpConditional(cc));
		}
		decoder.setBase(0xE9, new JumpToRegister(cpu -> cpu.hl));
		decoder.setDD(0xE9, new JumpToRegister(IndexRegister.IX::get));
		decoder.setFD(0xE9, new JumpToRegister(IndexRegister.IY::get));
	}
}
