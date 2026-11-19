package com.mj.z80.instructions;

import com.mj.z80.IndexRegister;
import com.mj.z80.Z80CPU;
import com.mj.z80.Z80Instruction;

/** ADD IX,rr / ADD IY,rr (DD/FD-prefixed $09/$19/$29/$39) - the identical
 * operation and flag shape as unprefixed ADD HL,rr (see
 * ArithmeticOperations.addToPair()), just targeting IX/IY instead of HL.
 * Real, documented, non-undocumented Z80 instructions - distinct from the
 * genuinely undocumented IXH/IXL-register tricks this project deliberately
 * skips. $29/$39's own source read happens before dest.set16() below, so
 * "ADD IX,IX" (source and dest the same register) reads the pre-add value
 * correctly rather than a half-updated one. */
public final class AddIndexRegisterPair implements Z80Instruction {
	@FunctionalInterface
	public interface OperandSource {
		int get(Z80CPU cpu);
	}

	private final IndexRegister dest;
	private final OperandSource source;

	public AddIndexRegisterPair(IndexRegister dest, OperandSource source) {
		this.dest = dest;
		this.source = source;
	}

	@Override
	public void execute(Z80CPU cpu) {
		int operand = source.get(cpu);
		ArithmeticOperations.addToPair(cpu, dest.get(cpu), operand);
	}
}
