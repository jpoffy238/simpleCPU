package com.mj.z80.instructions;

import com.mj.z80.RegisterPair;
import com.mj.z80.Z80CPU;
import com.mj.z80.Z80Instruction;

/** EXX ($D9) - swaps BC/DE/HL with their shadow counterparts (BC'/DE'/
 * HL'). Deliberately does not touch AF/AF' - that's EX AF,AF's own job,
 * a real split on real hardware. Affects no flags (nothing here is AF). */
public final class ExchangeShadowRegisters implements Z80Instruction {
	public static final ExchangeShadowRegisters INSTANCE = new ExchangeShadowRegisters();

	private ExchangeShadowRegisters() {
	}

	@Override
	public void execute(Z80CPU cpu) {
		swap(cpu.bc, cpu.bc2);
		swap(cpu.de, cpu.de2);
		swap(cpu.hl, cpu.hl2);
	}

	private static void swap(RegisterPair a, RegisterPair b) {
		int temp = a.get16();
		a.set16(b.get16());
		b.set16(temp);
	}
}
