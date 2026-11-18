package com.mj.z80.instructions;

import com.mj.z80.IndexRegister;
import com.mj.z80.Register8;
import com.mj.z80.Z80InstructionDecoder;

/** Populates the CB table's BIT/RES/SET opcodes - every bit position
 * (0-7) against every Register8, plus the (HL) slot and the DD-CB/FD-CB
 * indexed forms, same three-way split as RotateShiftTable. BIT
 * (`01 bbb rrr`) is read-only and sets flags (BitTestRegister/Memory/
 * Indexed); RES (`10 bbb rrr`) and SET (`11 bbb rrr`) both write back a
 * modified byte and affect no flag at all, so they share one class
 * (ModifyBitRegister/Memory/Indexed) parameterized by which bit
 * operation to apply - false for RES, true for SET. */
public final class BitTable {
	private BitTable() {
	}

	public static void populate(Z80InstructionDecoder decoder) {
		for (int bit = 0; bit < 8; bit++) {
			for (Register8 r : Register8.values()) {
				decoder.setCB(0x40 | (bit << 3) | r.code(), new BitTestRegister(bit, r));
				decoder.setCB(0x80 | (bit << 3) | r.code(), new ModifyBitRegister(bit, false, r));
				decoder.setCB(0xC0 | (bit << 3) | r.code(), new ModifyBitRegister(bit, true, r));
			}

			int memorySlot = (bit << 3) | 0x06;
			decoder.setCB(0x40 | memorySlot, new BitTestMemory(bit));
			decoder.setCB(0x80 | memorySlot, new ModifyBitMemory(bit, false));
			decoder.setCB(0xC0 | memorySlot, new ModifyBitMemory(bit, true));

			decoder.setDDCB(0x40 | memorySlot, new BitTestIndexed(bit, IndexRegister.IX));
			decoder.setDDCB(0x80 | memorySlot, new ModifyBitIndexed(bit, false, IndexRegister.IX));
			decoder.setDDCB(0xC0 | memorySlot, new ModifyBitIndexed(bit, true, IndexRegister.IX));

			decoder.setFDCB(0x40 | memorySlot, new BitTestIndexed(bit, IndexRegister.IY));
			decoder.setFDCB(0x80 | memorySlot, new ModifyBitIndexed(bit, false, IndexRegister.IY));
			decoder.setFDCB(0xC0 | memorySlot, new ModifyBitIndexed(bit, true, IndexRegister.IY));
		}
	}
}
