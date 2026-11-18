package com.mj.z80.instructions;

import com.mj.z80.RegisterPair16;
import com.mj.z80.Z80InstructionDecoder;

/** Populates every 16-bit direct-memory LD: LD HL,(nn)/LD (nn),HL ($2A/
 * $22, unprefixed), their LD IX/LD IY counterparts (same opcode bytes
 * under DD/FD), the ED-prefixed LD dd,(nn)/LD (nn),dd family covering
 * BC/DE/HL/SP uniformly (`ED 01 pp 011`-shaped: store = $43 | pp&lt;&lt;4,
 * load = $4B | pp&lt;&lt;4 - this reproduces real hardware's own redundant
 * second encoding for HL, ED $63/$6B, right alongside the short $22/$2A
 * forms, rather than needing to special-case HL out of the loop), and LD
 * SP,HL/LD SP,IX/LD SP,IY ($F9 unprefixed/DD/FD). */
public final class LoadRegisterPairMemoryTable {
	private LoadRegisterPairMemoryTable() {
	}

	public static void populate(Z80InstructionDecoder decoder) {
		decoder.setBase(0x2A, new LoadRegisterPairFromMemory((cpu, v) -> cpu.hl.set16(v)));
		decoder.setBase(0x22, new StoreRegisterPairToMemory(cpu -> cpu.hl.get16()));
		decoder.setDD(0x2A, new LoadRegisterPairFromMemory((cpu, v) -> cpu.ix.set16(v)));
		decoder.setDD(0x22, new StoreRegisterPairToMemory(cpu -> cpu.ix.get16()));
		decoder.setFD(0x2A, new LoadRegisterPairFromMemory((cpu, v) -> cpu.iy.set16(v)));
		decoder.setFD(0x22, new StoreRegisterPairToMemory(cpu -> cpu.iy.get16()));

		for (RegisterPair16 rr : RegisterPair16.values()) {
			decoder.setED(0x43 | (rr.code() << 4), new StoreRegisterPairToMemory(cpu -> rr.get16(cpu)));
			decoder.setED(0x4B | (rr.code() << 4), new LoadRegisterPairFromMemory((cpu, v) -> rr.set16(cpu, v)));
		}

		decoder.setBase(0xF9, new LoadSP(cpu -> cpu.hl.get16()));
		decoder.setDD(0xF9, new LoadSP(cpu -> cpu.ix.get16()));
		decoder.setFD(0xF9, new LoadSP(cpu -> cpu.iy.get16()));
	}
}
