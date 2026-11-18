package com.mj.z80.instructions;

import com.mj.z80.IndexRegister;
import com.mj.z80.RegisterPair16;
import com.mj.z80.Z80InstructionDecoder;

/** Populates the last of the base-table/ED-table instructions that don't
 * belong to any of the earlier named families: the EX variants (DE,HL;
 * AF,AF'; (SP),HL/IX/IY) and EXX, DAA, CPL, unprefixed ADD HL,rr
 * ($09/$19/$29/$39 - distinct from the ED-prefixed ADC HL,rr/SBC HL,rr in
 * EdArithmeticTable), and RLD/RRD. */
public final class MiscellaneousTable {
	private MiscellaneousTable() {
	}

	public static void populate(Z80InstructionDecoder decoder) {
		decoder.setBase(0x08, ExchangeAFShadow.INSTANCE);
		decoder.setBase(0xD9, ExchangeShadowRegisters.INSTANCE);
		decoder.setBase(0xEB, ExchangeDEHL.INSTANCE);

		decoder.setBase(0xE3, new ExchangeStackTop(cpu -> cpu.hl));
		decoder.setDD(0xE3, new ExchangeStackTop(IndexRegister.IX::get));
		decoder.setFD(0xE3, new ExchangeStackTop(IndexRegister.IY::get));

		decoder.setBase(0x27, DecimalAdjustAccumulator.INSTANCE);
		decoder.setBase(0x2F, ComplementAccumulator.INSTANCE);

		for (RegisterPair16 rr : RegisterPair16.values()) {
			decoder.setBase(0x09 | (rr.code() << 4), new AddHL(rr));
		}

		decoder.setED(0x6F, RotateDigitLeft.INSTANCE);
		decoder.setED(0x67, RotateDigitRight.INSTANCE);
	}
}
