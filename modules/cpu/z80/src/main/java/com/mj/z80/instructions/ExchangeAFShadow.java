package com.mj.z80.instructions;

import com.mj.z80.Z80CPU;
import com.mj.z80.Z80Instruction;

/** EX AF,AF' ($08) - swaps the main and shadow AF register pairs (which,
 * being the flags register's own pair, swaps the flags too - that's just
 * part of the swap, not this instruction "affecting flags" in the usual
 * sense). EXX swaps BC/DE/HL with their shadows the same way, but
 * deliberately never touches AF - that split is real hardware, not an
 * oversight here. */
public final class ExchangeAFShadow implements Z80Instruction {
	public static final ExchangeAFShadow INSTANCE = new ExchangeAFShadow();

	private ExchangeAFShadow() {
	}

	@Override
	public void execute(Z80CPU cpu) {
		int temp = cpu.af.get16();
		cpu.af.set16(cpu.af2.get16());
		cpu.af2.set16(temp);
	}
}
