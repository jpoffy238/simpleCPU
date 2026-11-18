package com.mj.z80.instructions;

import com.mj.z80.IndexRegister;
import com.mj.z80.Register8;
import com.mj.z80.Z80InstructionDecoder;

/** Populates the CB table's rotate/shift opcodes (`00 ooo rrr`) - every
 * RotateShiftOperation against every Register8, plus the (HL) slot
 * (rrr=110, which Register8 has no value for, so it's registered
 * separately - the same split LoadRegister/LoadFromMemory already drew
 * for the base table) - and the DD-CB/FD-CB indexed forms, which only
 * exist for that same (HL)-shaped slot; every other rrr value under
 * DD-CB/FD-CB is undocumented IXH/IXL-touching territory and is left
 * unregistered. */
public final class RotateShiftTable {
	private RotateShiftTable() {
	}

	public static void populate(Z80InstructionDecoder decoder) {
		for (RotateShiftOperation op : RotateShiftOperation.values()) {
			for (Register8 r : Register8.values()) {
				decoder.setCB((op.code() << 3) | r.code(), new RotateShiftRegister(op, r));
			}
			int memoryOpcode = (op.code() << 3) | 0x06;
			decoder.setCB(memoryOpcode, new RotateShiftMemory(op));
			decoder.setDDCB(memoryOpcode, new RotateShiftIndexed(op, IndexRegister.IX));
			decoder.setFDCB(memoryOpcode, new RotateShiftIndexed(op, IndexRegister.IY));
		}
	}
}
