package com.mj.z80;

/** The 8 condition codes JP/CALL/RET's `cc` field selects, in the real
 * opcode's own bit-field order (0-7). JR only supports the first four
 * (NZ/Z/NC/C) - a real hardware limitation (JR's own table picks those
 * four explicitly by name, not by slicing this enum's ordinal order,
 * since that's clearer about intent and doesn't quietly break if this
 * enum's declaration order ever changed for some other reason). */
public enum Condition {
	NZ(0, cpu -> (cpu.af.lo() & Flags.Z) == 0),
	Z(1, cpu -> (cpu.af.lo() & Flags.Z) != 0),
	NC(2, cpu -> (cpu.af.lo() & Flags.C) == 0),
	C(3, cpu -> (cpu.af.lo() & Flags.C) != 0),
	PO(4, cpu -> (cpu.af.lo() & Flags.PV) == 0),
	PE(5, cpu -> (cpu.af.lo() & Flags.PV) != 0),
	P(6, cpu -> (cpu.af.lo() & Flags.S) == 0),
	M(7, cpu -> (cpu.af.lo() & Flags.S) != 0);

	@FunctionalInterface
	private interface Test {
		boolean isMet(Z80CPU cpu);
	}

	private final int code;
	private final Test test;

	Condition(int code, Test test) {
		this.code = code;
		this.test = test;
	}

	public int code() {
		return code;
	}

	public boolean isMet(Z80CPU cpu) {
		return test.isMet(cpu);
	}
}
