package com.mj.z80.instructions;

import com.mj.z80.IndexRegister;
import com.mj.z80.RegisterPair16;
import com.mj.z80.Z80InstructionDecoder;

/** Populates the last of the base-table/ED-table instructions that don't
 * belong to any of the earlier named families: the EX variants (DE,HL;
 * AF,AF'; (SP),HL/IX/IY) and EXX, DAA, CPL, unprefixed ADD HL,rr
 * ($09/$19/$29/$39 - distinct from the ED-prefixed ADC HL,rr/SBC HL,rr in
 * EdArithmeticTable) and its DD/FD-prefixed ADD IX,rr/ADD IY,rr
 * counterparts (AddIndexRegisterPair - real, documented instructions, not
 * one of this project's deliberately-skipped undocumented IXH/IXL
 * tricks), RLD/RRD, and the unprefixed accumulator-only rotates RLCA/
 * RRCA/RLA/RRA ($07/$0F/$17/$1F - a different flag shape from the
 * CB-prefixed RLC/RRC/RL/RR on A, see AccumulatorRotateOperations), and
 * SCF/CCF ($37/$3F - CCF's own real quirk: H takes the OLD carry value,
 * not a plain 0, see ComplementCarryFlag). The ADD IX/IY,rr, RLCA/RRCA/
 * RLA/RRA, and SCF/CCF families were all real gaps found 2026-11-19
 * porting bdos.c/ccp.c to Z80 via SDCC - genuinely documented
 * instructions, missed earlier simply because no hand-written test
 * program had happened to need them yet. */
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

		decoder.setBase(0x07, RotateLeftAccumulatorCircular.INSTANCE);
		decoder.setBase(0x0F, RotateRightAccumulatorCircular.INSTANCE);
		decoder.setBase(0x17, RotateLeftAccumulator.INSTANCE);
		decoder.setBase(0x1F, RotateRightAccumulator.INSTANCE);

		decoder.setBase(0x37, SetCarryFlag.INSTANCE);
		decoder.setBase(0x3F, ComplementCarryFlag.INSTANCE);

		for (RegisterPair16 rr : RegisterPair16.values()) {
			decoder.setBase(0x09 | (rr.code() << 4), new AddHL(rr));
		}

		// ADD IX,rr/ADD IY,rr ($09/$19/$29/$39 under DD/FD) - real,
		// documented instructions (see AddIndexRegisterPair), not one of
		// this project's deliberately-skipped undocumented IXH/IXL tricks.
		// $29/$39 under each prefix means "add the index register to
		// itself"/"...to SP" - BC/DE are shared between both prefixes, but
		// the "HL slot" ($29) becomes IX or IY itself depending on prefix,
		// same convention already used elsewhere (LD IX,nn reusing LD
		// HL,nn's own $21).
		decoder.setDD(0x09, new AddIndexRegisterPair(IndexRegister.IX, cpu -> RegisterPair16.BC.get16(cpu)));
		decoder.setDD(0x19, new AddIndexRegisterPair(IndexRegister.IX, cpu -> RegisterPair16.DE.get16(cpu)));
		decoder.setDD(0x29, new AddIndexRegisterPair(IndexRegister.IX, cpu -> IndexRegister.IX.get(cpu).get16()));
		decoder.setDD(0x39, new AddIndexRegisterPair(IndexRegister.IX, cpu -> RegisterPair16.SP.get16(cpu)));
		decoder.setFD(0x09, new AddIndexRegisterPair(IndexRegister.IY, cpu -> RegisterPair16.BC.get16(cpu)));
		decoder.setFD(0x19, new AddIndexRegisterPair(IndexRegister.IY, cpu -> RegisterPair16.DE.get16(cpu)));
		decoder.setFD(0x29, new AddIndexRegisterPair(IndexRegister.IY, cpu -> IndexRegister.IY.get(cpu).get16()));
		decoder.setFD(0x39, new AddIndexRegisterPair(IndexRegister.IY, cpu -> RegisterPair16.SP.get16(cpu)));

		decoder.setED(0x6F, RotateDigitLeft.INSTANCE);
		decoder.setED(0x67, RotateDigitRight.INSTANCE);
	}
}
