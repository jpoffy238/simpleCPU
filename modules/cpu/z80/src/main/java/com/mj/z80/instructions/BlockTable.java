package com.mj.z80.instructions;

import com.mj.z80.Z80InstructionDecoder;

/** Populates the ED table's block instructions: LDI/LDD/LDIR/LDDR
 * ($A0/$A8/$B0/$B8) and CPI/CPD/CPIR/CPDR ($A1/$A9/$B1/$B9). */
public final class BlockTable {
	private BlockTable() {
	}

	public static void populate(Z80InstructionDecoder decoder) {
		decoder.setED(0xA0, new BlockCopy(1));
		decoder.setED(0xA8, new BlockCopy(-1));
		decoder.setED(0xB0, new BlockCopyRepeat(1));
		decoder.setED(0xB8, new BlockCopyRepeat(-1));

		decoder.setED(0xA1, new BlockCompare(1));
		decoder.setED(0xA9, new BlockCompare(-1));
		decoder.setED(0xB1, new BlockCompareRepeat(1));
		decoder.setED(0xB9, new BlockCompareRepeat(-1));
	}
}
