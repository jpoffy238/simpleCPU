package com.mj.z80.instructions;

import com.mj.z80.Z80CPU;

/** The seven documented CB-prefixed rotate/shift operations, in the real
 * opcode's own `ooo` bit-field order (0-7, with 6 skipped - that's SLL,
 * undocumented, deliberately not implemented, see
 * RotateShiftOperations). */
public enum RotateShiftOperation {
	RLC(0, RotateShiftOperations::rlc),
	RRC(1, RotateShiftOperations::rrc),
	RL(2, RotateShiftOperations::rl),
	RR(3, RotateShiftOperations::rr),
	SLA(4, RotateShiftOperations::sla),
	SRA(5, RotateShiftOperations::sra),
	// 6 = SLL, undocumented, intentionally absent
	SRL(7, RotateShiftOperations::srl);

	@FunctionalInterface
	private interface Apply {
		int apply(Z80CPU cpu, int operand);
	}

	private final int code;
	private final Apply apply;

	RotateShiftOperation(int code, Apply apply) {
		this.code = code;
		this.apply = apply;
	}

	public int code() {
		return code;
	}

	public int apply(Z80CPU cpu, int operand) {
		return apply.apply(cpu, operand);
	}
}
