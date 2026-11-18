package com.mj.z80.instructions;

import java.util.function.BiConsumer;

import com.mj.z80.IndexRegister;
import com.mj.z80.Register8;
import com.mj.z80.Z80Instruction;
import com.mj.z80.Z80InstructionDecoder;

/** Populates the DD and FD tables' LD r,(I+d) (`01 rrr 110`), LD (I+d),r
 * (`01 110 rrr`), and LD (I+d),n ($36) opcodes - the indexed counterparts
 * of LoadMemoryTable's plain-HL family, same opcode bytes, just under a
 * prefix and with an extra displacement. One method body (populateFor)
 * builds both tables, parameterized by which IndexRegister constant each
 * instruction closes over for addressing, and which of the decoder's own
 * setDD/setFD methods actually registers the result - the "one builder,
 * two closures" approach decided for the fetch/decode loop itself. */
public final class LoadIndexedTable {
	private LoadIndexedTable() {
	}

	public static void populate(Z80InstructionDecoder decoder) {
		populateFor(IndexRegister.IX, decoder::setDD);
		populateFor(IndexRegister.IY, decoder::setFD);
	}

	private static void populateFor(IndexRegister index, BiConsumer<Integer, Z80Instruction> table) {
		for (Register8 r : Register8.values()) {
			table.accept(0x40 | (r.code() << 3) | 0x06, new LoadFromIndexed(r, index));
			table.accept(0x70 | r.code(), new StoreToIndexed(index, r));
		}
		table.accept(0x36, new StoreImmediateToIndexed(index));
	}
}
