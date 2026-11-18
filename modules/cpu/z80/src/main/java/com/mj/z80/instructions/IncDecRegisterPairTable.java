package com.mj.z80.instructions;

import com.mj.z80.IndexRegister;
import com.mj.z80.RegisterPair16;
import com.mj.z80.Z80InstructionDecoder;

/** Populates INC/DEC BC/DE/HL/SP (`00 dd 0011`/`00 dd 1011`, $03-$33/
 * $0B-$3B) and INC/DEC IX/IY (same $23/$2B bytes under DD/FD, mirroring
 * LD IX,nn reusing LD HL,nn's own $21). */
public final class IncDecRegisterPairTable {
	private IncDecRegisterPairTable() {
	}

	public static void populate(Z80InstructionDecoder decoder) {
		for (RegisterPair16 rr : RegisterPair16.values()) {
			decoder.setBase(0x03 | (rr.code() << 4), new IncrementRegisterPair(cpu -> rr.get16(cpu), (cpu, v) -> rr.set16(cpu, v)));
			decoder.setBase(0x0B | (rr.code() << 4), new DecrementRegisterPair(cpu -> rr.get16(cpu), (cpu, v) -> rr.set16(cpu, v)));
		}
		decoder.setDD(0x23, new IncrementRegisterPair(cpu -> IndexRegister.IX.get(cpu).get16(), (cpu, v) -> IndexRegister.IX.get(cpu).set16(v)));
		decoder.setDD(0x2B, new DecrementRegisterPair(cpu -> IndexRegister.IX.get(cpu).get16(), (cpu, v) -> IndexRegister.IX.get(cpu).set16(v)));
		decoder.setFD(0x23, new IncrementRegisterPair(cpu -> IndexRegister.IY.get(cpu).get16(), (cpu, v) -> IndexRegister.IY.get(cpu).set16(v)));
		decoder.setFD(0x2B, new DecrementRegisterPair(cpu -> IndexRegister.IY.get(cpu).get16(), (cpu, v) -> IndexRegister.IY.get(cpu).set16(v)));
	}
}
