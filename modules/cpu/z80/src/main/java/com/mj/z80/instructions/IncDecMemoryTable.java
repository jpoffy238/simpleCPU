package com.mj.z80.instructions;

import com.mj.z80.IndexRegister;
import com.mj.z80.Z80InstructionDecoder;

/** Populates INC/DEC (HL) ($34/$35, unprefixed) and INC/DEC (IX+d)/
 * (IY+d) (same $34/$35 bytes under DD/FD). */
public final class IncDecMemoryTable {
	private IncDecMemoryTable() {
	}

	public static void populate(Z80InstructionDecoder decoder) {
		decoder.setBase(0x34, IncrementMemory.INSTANCE);
		decoder.setBase(0x35, DecrementMemory.INSTANCE);
		decoder.setDD(0x34, new IncrementIndexed(IndexRegister.IX));
		decoder.setDD(0x35, new DecrementIndexed(IndexRegister.IX));
		decoder.setFD(0x34, new IncrementIndexed(IndexRegister.IY));
		decoder.setFD(0x35, new DecrementIndexed(IndexRegister.IY));
	}
}
