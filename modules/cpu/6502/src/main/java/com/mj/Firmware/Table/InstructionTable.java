package com.mj.Firmware.Table;

import static com.mj.Firmware.Table.AddrMode.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * One row per opcode: opcode byte, operation, addressing mode, base cycles.
 * The {@code OpCodes} enum registers these with the decoder.
 */
public final class InstructionTable {
	private InstructionTable() {
	}

	private static final List<TableInstruction> ALL = new ArrayList<>();

	private static TableInstruction row(int opcode, Operation op, AddrMode mode, int cycles) {
		TableInstruction t = new TableInstruction(opcode, op, mode, cycles);
		ALL.add(t);
		return t;
	}

	public static List<TableInstruction> all() {
		return Collections.unmodifiableList(ALL);
	}

	// ---- LDA -------------------------------------------------------------------
	public static final TableInstruction LDA_IMM = row(0xA9, Operations.LDA, IMMEDIATE, 2);
	public static final TableInstruction LDA_ZP = row(0xA5, Operations.LDA, ZERO_PAGE, 3);
	public static final TableInstruction LDA_ZX = row(0xB5, Operations.LDA, ZERO_PAGE_X, 4);
	public static final TableInstruction LDA_ABS = row(0xAD, Operations.LDA, ABSOLUTE, 4);
	public static final TableInstruction LDA_ABSX = row(0xBD, Operations.LDA, ABSOLUTE_X, 4);
	public static final TableInstruction LDA_ABSY = row(0xB9, Operations.LDA, ABSOLUTE_Y, 4);
	public static final TableInstruction LDA_INDX = row(0xA1, Operations.LDA, INDEXED_INDIRECT, 6);
	public static final TableInstruction LDA_INDY = row(0xB1, Operations.LDA, INDIRECT_INDEXED, 5);
}
