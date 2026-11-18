package com.mj.z80.instructions;

import com.mj.z80.Z80CPU;
import com.mj.z80.Z80Instruction;

/** EX DE,HL ($EB) - swaps DE and HL. Affects no flags. */
public final class ExchangeDEHL implements Z80Instruction {
	public static final ExchangeDEHL INSTANCE = new ExchangeDEHL();

	private ExchangeDEHL() {
	}

	@Override
	public void execute(Z80CPU cpu) {
		int temp = cpu.de.get16();
		cpu.de.set16(cpu.hl.get16());
		cpu.hl.set16(temp);
	}
}
