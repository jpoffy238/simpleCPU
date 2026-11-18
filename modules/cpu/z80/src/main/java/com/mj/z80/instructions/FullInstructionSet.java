package com.mj.z80.instructions;

import com.mj.z80.Z80InstructionDecoder;

/** Populates every instruction family this project implements onto one
 * decoder - every other *Table class covers one family/tier in
 * isolation (that's what each instruction test exercises), but any real
 * program needs them all at once. One place to add a new family's own
 * populate() call when it's built, rather than every real caller having
 * to remember the whole list for itself. */
public final class FullInstructionSet {
	private FullInstructionSet() {
	}

	public static void populate(Z80InstructionDecoder decoder) {
		LoadRegisterTable.populate(decoder);
		LoadImmediateTable.populate(decoder);
		LoadMemoryTable.populate(decoder);
		LoadIndexedTable.populate(decoder);
		LoadRegisterPairImmediateTable.populate(decoder);
		LoadRegisterPairMemoryTable.populate(decoder);
		SpecialLoadTable.populate(decoder);
		PushPopTable.populate(decoder);
		AluRegisterTable.populate(decoder);
		AluImmediateTable.populate(decoder);
		AluMemoryTable.populate(decoder);
		AluIndexedTable.populate(decoder);
		JumpTable.populate(decoder);
		JumpRelativeTable.populate(decoder);
		CallTable.populate(decoder);
		ReturnTable.populate(decoder);
		RestartTable.populate(decoder);
		IncDecRegisterTable.populate(decoder);
		IncDecMemoryTable.populate(decoder);
		IncDecRegisterPairTable.populate(decoder);
		RotateShiftTable.populate(decoder);
		BitTable.populate(decoder);
		EdArithmeticTable.populate(decoder);
		BlockTable.populate(decoder);
		PortTable.populate(decoder);
		PortRegisterTable.populate(decoder);
		InterruptTable.populate(decoder);
		MiscellaneousTable.populate(decoder);
	}
}
