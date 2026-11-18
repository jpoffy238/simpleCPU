package com.mj.z80.instructions;

import com.mj.z80.Z80CPU;
import com.mj.z80.Z80Instruction;

/** EI ($FB) - sets both interrupt flip-flops. Real hardware doesn't
 * actually start accepting interrupts until the instruction immediately
 * following this one has finished - see Z80CPU.suppressInterruptCheckOnce
 * - which is what protects the common EI;RET ISR-exit idiom from being
 * re-interrupted before the RET executes. */
public final class EnableInterrupts implements Z80Instruction {
	public static final EnableInterrupts INSTANCE = new EnableInterrupts();

	private EnableInterrupts() {
	}

	@Override
	public void execute(Z80CPU cpu) {
		cpu.iff1 = true;
		cpu.iff2 = true;
		cpu.suppressInterruptCheckOnce = true;
	}
}
