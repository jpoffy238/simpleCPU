package com.mj.z80.instructions;

import com.mj.z80.Z80InstructionDecoder;

/** Populates the handful of LD opcodes that don't fit any of the regular
 * families: A's own direct-memory addressing (LD A,(nn) $3A, LD (nn),A
 * $32 - no other 8-bit register has an equivalent), and the write
 * direction of the I/R registers (LD I,A ED $47, LD R,A ED $4F).
 *
 * LD A,I (ED $57) and LD A,R (ED $5F) are deliberately NOT populated here -
 * unlike every other LD on this CPU, they set real flags (S/Z from the
 * loaded byte, H/N cleared, P/V from IFF2, the interrupt-enable flip-flop)
 * - which doesn't exist until EI/DI/interrupt handling does. Add them once
 * that's built, not before. */
public final class SpecialLoadTable {
	private SpecialLoadTable() {
	}

	public static void populate(Z80InstructionDecoder decoder) {
		decoder.setBase(0x3A, LoadAFromMemory.INSTANCE);
		decoder.setBase(0x32, StoreAToMemory.INSTANCE);
		decoder.setED(0x47, LoadIFromA.INSTANCE);
		decoder.setED(0x4F, LoadRFromA.INSTANCE);
	}
}
