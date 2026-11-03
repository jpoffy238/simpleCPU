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

	// ---- LDX -------------------------------------------------------------------
	public static final TableInstruction LDX_IMM = row(0xA2, Operations.LDX, IMMEDIATE, 2);
	public static final TableInstruction LDX_ZP = row(0xA6, Operations.LDX, ZERO_PAGE, 3);
	public static final TableInstruction LDX_ZPY = row(0xB6, Operations.LDX, ZERO_PAGE_Y, 4);
	public static final TableInstruction LDX_ABS = row(0xAE, Operations.LDX, ABSOLUTE, 4);
	public static final TableInstruction LDX_ABSY = row(0xBE, Operations.LDX, ABSOLUTE_Y, 4);

	// ---- LDY -------------------------------------------------------------------
	public static final TableInstruction LDY_IMM = row(0xA0, Operations.LDY, IMMEDIATE, 2);
	public static final TableInstruction LDY_ZP = row(0xA4, Operations.LDY, ZERO_PAGE, 3);
	public static final TableInstruction LDY_ZPX = row(0xB4, Operations.LDY, ZERO_PAGE_X, 4);
	public static final TableInstruction LDY_ABS = row(0xAC, Operations.LDY, ABSOLUTE, 4);
	public static final TableInstruction LDY_ABSX = row(0xBC, Operations.LDY, ABSOLUTE_X, 4);

	// ---- STA -------------------------------------------------------------------
	public static final TableInstruction STA_ZP = row(0x85, Operations.STA, ZERO_PAGE, 3);
	public static final TableInstruction STA_ZPX = row(0x95, Operations.STA, ZERO_PAGE_X, 4);
	public static final TableInstruction STA_ABS = row(0x8D, Operations.STA, ABSOLUTE, 4);
	public static final TableInstruction STA_ABX = row(0x9D, Operations.STA, ABSOLUTE_X, 5);
	public static final TableInstruction STA_ABY = row(0x99, Operations.STA, ABSOLUTE_Y, 5);
	public static final TableInstruction STA_INX = row(0x81, Operations.STA, INDEXED_INDIRECT, 6);
	public static final TableInstruction STA_INY = row(0x91, Operations.STA, INDIRECT_INDEXED, 6);

	// ---- STX -------------------------------------------------------------------
	public static final TableInstruction STX_ZP = row(0x86, Operations.STX, ZERO_PAGE, 3);
	public static final TableInstruction STX_ZPY = row(0x96, Operations.STX, ZERO_PAGE_Y, 4);
	public static final TableInstruction STX_ABS = row(0x8E, Operations.STX, ABSOLUTE, 4);

	// ---- STY -------------------------------------------------------------------
	public static final TableInstruction STY_ZP = row(0x84, Operations.STY, ZERO_PAGE, 3);
	public static final TableInstruction STY_ZPX = row(0x94, Operations.STY, ZERO_PAGE_X, 4);
	public static final TableInstruction STY_ABS = row(0x8C, Operations.STY, ABSOLUTE, 4);

	// ---- AND ------------------------------------------------------------
	public static final TableInstruction AND_IMM = row(0x29, Operations.AND, IMMEDIATE, 2);
	public static final TableInstruction AND_ZP = row(0x25, Operations.AND, ZERO_PAGE, 3);
	public static final TableInstruction AND_ZPX = row(0x35, Operations.AND, ZERO_PAGE_X, 4);
	public static final TableInstruction AND_ABS = row(0x2D, Operations.AND, ABSOLUTE, 4);
	public static final TableInstruction AND_ABX = row(0x3D, Operations.AND, ABSOLUTE_X, 4);
	public static final TableInstruction AND_ABY = row(0x39, Operations.AND, ABSOLUTE_Y, 4);
	public static final TableInstruction AND_INX = row(0x21, Operations.AND, INDEXED_INDIRECT, 6);
	public static final TableInstruction AND_INY = row(0x31, Operations.AND, INDIRECT_INDEXED, 5);

	// ---- ORA ------------------------------------------------------------
	public static final TableInstruction ORA_IMM = row(0x09, Operations.ORA, IMMEDIATE, 2);
	public static final TableInstruction ORA_ZP = row(0x05, Operations.ORA, ZERO_PAGE, 3);
	public static final TableInstruction ORA_ZPX = row(0x15, Operations.ORA, ZERO_PAGE_X, 4);
	public static final TableInstruction ORA_ABS = row(0x0D, Operations.ORA, ABSOLUTE, 4);
	public static final TableInstruction ORA_ABSX = row(0x1D, Operations.ORA, ABSOLUTE_X, 4);
	public static final TableInstruction ORA_ABSY = row(0x19, Operations.ORA, ABSOLUTE_Y, 4);
	public static final TableInstruction ORA_INX = row(0x01, Operations.ORA, INDEXED_INDIRECT, 6);
	public static final TableInstruction ORA_INY = row(0x11, Operations.ORA, INDIRECT_INDEXED, 5);

	// ---- EOR ------------------------------------------------------------
	public static final TableInstruction EOR_IMM = row(0x49, Operations.EOR, IMMEDIATE, 2);
	public static final TableInstruction EOR_ZP = row(0x45, Operations.EOR, ZERO_PAGE, 3);
	public static final TableInstruction EOR_ZPX = row(0x55, Operations.EOR, ZERO_PAGE_X, 4);
	public static final TableInstruction EOR_ABS = row(0x4D, Operations.EOR, ABSOLUTE, 4);
	public static final TableInstruction EOR_ABSX = row(0x5D, Operations.EOR, ABSOLUTE_X, 4);
	public static final TableInstruction EOR_ABSY = row(0x59, Operations.EOR, ABSOLUTE_Y, 4);
	public static final TableInstruction EOR_INX = row(0x41, Operations.EOR, INDEXED_INDIRECT, 6);
	public static final TableInstruction EOR_INY = row(0x51, Operations.EOR, INDIRECT_INDEXED, 5);

	// ---- INC / DEC -------------------------------------------------------------
	public static final TableInstruction INC_ZP = row(0xE6, Operations.INC, ZERO_PAGE, 5);
	public static final TableInstruction INC_ZPX = row(0xF6, Operations.INC, ZERO_PAGE_X, 6);
	public static final TableInstruction INC_ABS = row(0xEE, Operations.INC, ABSOLUTE, 6);
	public static final TableInstruction INC_ABX = row(0xFE, Operations.INC, ABSOLUTE_X, 7);
	public static final TableInstruction DEC_ZP = row(0xC6, Operations.DEC, ZERO_PAGE, 5);
	public static final TableInstruction DEC_ZPX = row(0xD6, Operations.DEC, ZERO_PAGE_X, 6);
	public static final TableInstruction DEC_ABS = row(0xCE, Operations.DEC, ABSOLUTE, 6);
	public static final TableInstruction DEC_ABX = row(0xDE, Operations.DEC, ABSOLUTE_X, 7);

	// ---- register increments / decrements --------------------------------------
	public static final TableInstruction INX = row(0xE8, Operations.INX, IMPLIED, 2);
	public static final TableInstruction INY = row(0xC8, Operations.INY, IMPLIED, 2);
	public static final TableInstruction DEX = row(0xCA, Operations.DEX, IMPLIED, 2);
	public static final TableInstruction DEY = row(0x88, Operations.DEY, IMPLIED, 2);

	// ---- Shifts and rotates ------------------------------------------------------
	public static final TableInstruction ASL = row(0x0A, Operations.ASL, ACCUMULATOR, 2);
	public static final TableInstruction ASL_ZP = row(0x06, Operations.ASL, ZERO_PAGE, 5);
	public static final TableInstruction ASL_ZPX = row(0x16, Operations.ASL, ZERO_PAGE_X, 6);
	public static final TableInstruction ASL_ABS = row(0x0E, Operations.ASL, ABSOLUTE, 6);
	public static final TableInstruction ASL_ABSX = row(0x1E, Operations.ASL, ABSOLUTE_X, 7);
	public static final TableInstruction LSR_ACC = row(0x4A, Operations.LSR, ACCUMULATOR, 2);
	public static final TableInstruction LSR_ZP = row(0x46, Operations.LSR, ZERO_PAGE, 5);
	public static final TableInstruction LSR_ZPX = row(0x56, Operations.LSR, ZERO_PAGE_X, 6);
	public static final TableInstruction LSR_ABS = row(0x4E, Operations.LSR, ABSOLUTE, 6);
	public static final TableInstruction LSR_ABX = row(0x5E, Operations.LSR, ABSOLUTE_X, 7);
	public static final TableInstruction ROL_ACC = row(0x2A, Operations.ROL, ACCUMULATOR, 2);
	public static final TableInstruction ROL_ZP = row(0x26, Operations.ROL, ZERO_PAGE, 5);
	public static final TableInstruction ROL_ZPX = row(0x36, Operations.ROL, ZERO_PAGE_X, 6);
	public static final TableInstruction ROL_ABS = row(0x2E, Operations.ROL, ABSOLUTE, 6);
	public static final TableInstruction ROL_ABX = row(0x3E, Operations.ROL, ABSOLUTE_X, 7);
	public static final TableInstruction ROR_ACC = row(0x6A, Operations.ROR, ACCUMULATOR, 2);
	public static final TableInstruction ROR_ZP = row(0x66, Operations.ROR, ZERO_PAGE, 5);
	public static final TableInstruction ROR_ZPX = row(0x76, Operations.ROR, ZERO_PAGE_X, 6);
	public static final TableInstruction ROR_ABS = row(0x6E, Operations.ROR, ABSOLUTE, 6);
	public static final TableInstruction ROR_ABX = row(0x7E, Operations.ROR, ABSOLUTE_X, 7);

	// ---- CMP / CPX / CPY -------------------------------------------------------
	public static final TableInstruction CMP_IMM = row(0xC9, Operations.CMP, IMMEDIATE, 2);
	public static final TableInstruction CMP_ZP = row(0xC5, Operations.CMP, ZERO_PAGE, 3);
	public static final TableInstruction CMP_ZPX = row(0xD5, Operations.CMP, ZERO_PAGE_X, 4);
	public static final TableInstruction CMP_ABS = row(0xCD, Operations.CMP, ABSOLUTE, 4);
	public static final TableInstruction CMP_ABX = row(0xDD, Operations.CMP, ABSOLUTE_X, 4);
	public static final TableInstruction CMP_ABY = row(0xD9, Operations.CMP, ABSOLUTE_Y, 4);
	public static final TableInstruction CMP_INX = row(0xC1, Operations.CMP, INDEXED_INDIRECT, 6);
	public static final TableInstruction CMP_INY = row(0xD1, Operations.CMP, INDIRECT_INDEXED, 5);
	public static final TableInstruction CPX_IMM = row(0xE0, Operations.CPX, IMMEDIATE, 2);
	public static final TableInstruction CPX_ZP = row(0xE4, Operations.CPX, ZERO_PAGE, 3);
	public static final TableInstruction CPX_ABS = row(0xEC, Operations.CPX, ABSOLUTE, 4);
	public static final TableInstruction CPY_IMM = row(0xC0, Operations.CPY, IMMEDIATE, 2);
	public static final TableInstruction CPY_ZP = row(0xC4, Operations.CPY, ZERO_PAGE, 3);
	public static final TableInstruction CPY_ABS = row(0xCC, Operations.CPY, ABSOLUTE, 4);

	// ---- ADC / SBC -------------------------------------------------------------
	public static final TableInstruction ADC_IMM = row(0x69, Operations.ADC, IMMEDIATE, 2);
	public static final TableInstruction ADC_ZP = row(0x65, Operations.ADC, ZERO_PAGE, 3);
	public static final TableInstruction ADC_ZPX = row(0x75, Operations.ADC, ZERO_PAGE_X, 4);
	public static final TableInstruction ADC_ABS = row(0x6D, Operations.ADC, ABSOLUTE, 4);
	public static final TableInstruction ADC_ABSX = row(0x7D, Operations.ADC, ABSOLUTE_X, 4);
	public static final TableInstruction ADC_ABSY = row(0x79, Operations.ADC, ABSOLUTE_Y, 4);
	public static final TableInstruction ADC_INX = row(0x61, Operations.ADC, INDEXED_INDIRECT, 6);
	public static final TableInstruction ADC_INY = row(0x71, Operations.ADC, INDIRECT_INDEXED, 5);
	public static final TableInstruction SBC_IMM = row(0xE9, Operations.SBC, IMMEDIATE, 2);
	public static final TableInstruction SBC_ZP = row(0xE5, Operations.SBC, ZERO_PAGE, 3);
	public static final TableInstruction SBC_ZPX = row(0xF5, Operations.SBC, ZERO_PAGE_X, 4);
	public static final TableInstruction SBC_ABS = row(0xED, Operations.SBC, ABSOLUTE, 4);
	public static final TableInstruction SBC_ABSX = row(0xFD, Operations.SBC, ABSOLUTE_X, 4);
	public static final TableInstruction SBC_ABSY = row(0xF9, Operations.SBC, ABSOLUTE_Y, 4);
	public static final TableInstruction SBC_INX = row(0xE1, Operations.SBC, INDEXED_INDIRECT, 6);
	public static final TableInstruction SBC_INY = row(0xF1, Operations.SBC, INDIRECT_INDEXED, 5);

	// ---- BIT, transfers, flags, NOP and stack operations ---------------------------
	public static final TableInstruction BIT_ZP = row(0x24, Operations.BIT, ZERO_PAGE, 3);
	public static final TableInstruction BIT_ABS = row(0x2C, Operations.BIT, ABSOLUTE, 4);
	public static final TableInstruction TAX = row(0xAA, Operations.TAX, IMPLIED, 2);
	public static final TableInstruction TAY = row(0xA8, Operations.TAY, IMPLIED, 2);
	public static final TableInstruction TXA = row(0x8A, Operations.TXA, IMPLIED, 2);
	public static final TableInstruction TYA = row(0x98, Operations.TYA, IMPLIED, 2);
	public static final TableInstruction TSX = row(0xBA, Operations.TSX, IMPLIED, 2);
	public static final TableInstruction TXS = row(0x9A, Operations.TXS, IMPLIED, 2);
	public static final TableInstruction CLC = row(0x18, Operations.CLC, IMPLIED, 2);
	public static final TableInstruction SEC = row(0x38, Operations.SEC, IMPLIED, 2);
	public static final TableInstruction CLI = row(0x58, Operations.CLI, IMPLIED, 2);
	public static final TableInstruction SEI = row(0x78, Operations.SEI, IMPLIED, 2);
	public static final TableInstruction CLD = row(0xD8, Operations.CLD, IMPLIED, 2);
	public static final TableInstruction SED = row(0xF8, Operations.SED, IMPLIED, 2);
	public static final TableInstruction CLV = row(0xB8, Operations.CLV, IMPLIED, 2);
	public static final TableInstruction NOP = row(0xEA, Operations.NOP, IMPLIED, 2);
	public static final TableInstruction PHA = row(0x48, Operations.PHA, IMPLIED, 3);
	public static final TableInstruction PHP = row(0x08, Operations.PHP, IMPLIED, 3);
	public static final TableInstruction PLA = row(0x68, Operations.PLA, IMPLIED, 4);
	public static final TableInstruction PLP = row(0x28, Operations.PLP, IMPLIED, 4);
}
