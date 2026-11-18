package com.mj.z80.instructions;

import com.mj.z80.Z80CPU;
import com.mj.z80.Z80Instruction;

/** DI ($F3) - clears both interrupt flip-flops; maskable interrupts (and
 * NMI's own use of IFF2 via RETN) are blocked until EI runs again. */
public final class DisableInterrupts implements Z80Instruction {
	public static final DisableInterrupts INSTANCE = new DisableInterrupts();

	private DisableInterrupts() {
	}

	@Override
	public void execute(Z80CPU cpu) {
		cpu.iff1 = false;
		cpu.iff2 = false;
	}
}
