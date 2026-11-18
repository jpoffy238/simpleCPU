package com.mj.z80.instructions;

import com.mj.z80.IndexRegister;
import com.mj.z80.RegisterPair16;
import com.mj.z80.Z80InstructionDecoder;

/** Populates LD dd,nn (`00 dd 0001`, $01/$11/$21/$31 for BC/DE/HL/SP) in
 * the base table, plus LD IX,nn/LD IY,nn - the same $21 opcode byte as LD
 * HL,nn, just under the DD/FD prefix, so each is its own single
 * decoder.setDD()/setFD() call rather than folding into the BC/DE/HL/SP
 * loop (which only ever touches the base table). */
public final class LoadRegisterPairImmediateTable {
	private LoadRegisterPairImmediateTable() {
	}

	public static void populate(Z80InstructionDecoder decoder) {
		for (RegisterPair16 rr : RegisterPair16.values()) {
			decoder.setBase(0x01 | (rr.code() << 4), new LoadRegisterPairImmediate(rr));
		}
		decoder.setDD(0x21, new LoadIndexImmediate(IndexRegister.IX));
		decoder.setFD(0x21, new LoadIndexImmediate(IndexRegister.IY));
	}
}
