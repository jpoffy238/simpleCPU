package com.mj.z80.instructions;

import com.mj.z80.IndexRegister;
import com.mj.z80.StackRegisterPair;
import com.mj.z80.Z80InstructionDecoder;

/** Populates PUSH qq/POP qq (`11 qq 0101`/`11 qq 0001`, $C5-$F5/$C1-$F1 for
 * BC/DE/HL/AF) in the base table, plus PUSH IX/POP IX and PUSH IY/POP IY -
 * the same $E5/$E1 opcode bytes as PUSH HL/POP HL, just under the DD/FD
 * prefix, same reasoning as LD IX,nn/LD IY,nn reusing LD HL,nn's own $21. */
public final class PushPopTable {
	private PushPopTable() {
	}

	public static void populate(Z80InstructionDecoder decoder) {
		for (StackRegisterPair rr : StackRegisterPair.values()) {
			decoder.setBase(0xC5 | (rr.code() << 4), new Push(rr::get));
			decoder.setBase(0xC1 | (rr.code() << 4), new Pop(rr::get));
		}
		decoder.setDD(0xE5, new Push(IndexRegister.IX::get));
		decoder.setDD(0xE1, new Pop(IndexRegister.IX::get));
		decoder.setFD(0xE5, new Push(IndexRegister.IY::get));
		decoder.setFD(0xE1, new Pop(IndexRegister.IY::get));
	}
}
