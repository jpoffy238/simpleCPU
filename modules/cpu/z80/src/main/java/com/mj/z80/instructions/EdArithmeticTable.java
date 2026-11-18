package com.mj.z80.instructions;

import com.mj.z80.RegisterPair16;
import com.mj.z80.Z80InstructionDecoder;

/** Populates the ED table's arithmetic opcodes: NEG ($44), ADC HL,rr
 * (`01 pp 1010`, $4A-$7A), and SBC HL,rr (`01 pp 0010`, $42-$72) for
 * BC/DE/HL/SP. */
public final class EdArithmeticTable {
	private EdArithmeticTable() {
	}

	public static void populate(Z80InstructionDecoder decoder) {
		decoder.setED(0x44, Negate.INSTANCE);
		for (RegisterPair16 rr : RegisterPair16.values()) {
			decoder.setED(0x4A | (rr.code() << 4), new AddCarryHL(rr));
			decoder.setED(0x42 | (rr.code() << 4), new SubtractCarryHL(rr));
		}
	}
}
