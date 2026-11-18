package com.mj.z80.instructions;

import java.util.function.BiConsumer;

import com.mj.z80.IndexRegister;
import com.mj.z80.Z80Instruction;
import com.mj.z80.Z80InstructionDecoder;

/** Populates the DD and FD tables' indexed-memory ALU opcodes (`10 ooo
 * 110`, same bytes as AluMemoryTable's own, just under a prefix and with
 * an extra displacement) - one method body (populateFor) builds both
 * tables, same "one builder, two closures" approach as LoadIndexedTable. */
public final class AluIndexedTable {
	private AluIndexedTable() {
	}

	public static void populate(Z80InstructionDecoder decoder) {
		populateFor(IndexRegister.IX, decoder::setDD);
		populateFor(IndexRegister.IY, decoder::setFD);
	}

	private static void populateFor(IndexRegister index, BiConsumer<Integer, Z80Instruction> table) {
		for (AluOperation op : AluOperation.values()) {
			int opcode = 0x80 | (op.code() << 3) | 0x06;
			table.accept(opcode, new AluIndexed(op, index));
		}
	}
}
