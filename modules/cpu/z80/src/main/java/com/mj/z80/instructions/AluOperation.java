package com.mj.z80.instructions;

import com.mj.z80.Z80CPU;

/** The eight 8-bit ALU operations sharing the `10 ooo rrr`/`11 ooo 110`
 * opcode shapes - ADD, ADC, SUB, SBC, AND, XOR, OR, CP, in that order
 * (the real Z80's own `ooo` encoding, 0-7). ADD/ADC/SUB/SBC/CP share
 * ArithmeticOperations' math; AND/XOR/OR share LogicalOperations' -
 * see their class comments for why those are genuinely different shapes,
 * not one generalized over the other. CP uses the same subtraction as
 * SUB but with writeBack=false - it computes and sets flags exactly like
 * SUB, just never touches A itself. */
public enum AluOperation {
	ADD(0, (cpu, operand) -> ArithmeticOperations.addSub(cpu, operand, false, false, true)),
	ADC(1, (cpu, operand) -> ArithmeticOperations.addSub(cpu, operand, true, false, true)),
	SUB(2, (cpu, operand) -> ArithmeticOperations.addSub(cpu, operand, false, true, true)),
	SBC(3, (cpu, operand) -> ArithmeticOperations.addSub(cpu, operand, true, true, true)),
	AND(4, LogicalOperations::and),
	XOR(5, LogicalOperations::xor),
	OR(6, LogicalOperations::or),
	CP(7, (cpu, operand) -> ArithmeticOperations.addSub(cpu, operand, false, true, false));

	@FunctionalInterface
	private interface Apply {
		void apply(Z80CPU cpu, int operand);
	}

	private final int code;
	private final Apply apply;

	AluOperation(int code, Apply apply) {
		this.code = code;
		this.apply = apply;
	}

	public int code() {
		return code;
	}

	public void apply(Z80CPU cpu, int operand) {
		apply.apply(cpu, operand);
	}
}
