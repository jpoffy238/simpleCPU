package com.mj.z80.instructions;

import com.mj.z80.Z80CPU;
import com.mj.z80.Z80Instruction;

/** IM 0/1/2 (ED $46/$56/$5E) - sets Z80CPU.interruptMode, read later by
 * Z80CPU.serviceInterrupts() when a maskable interrupt actually needs
 * handling. The undocumented duplicate opcodes ($4E/$66/$6E also mean
 * IM 0, $76 also means IM 1, $7E also means IM 2 on real hardware) are
 * deliberately left unregistered - same posture as every other
 * undocumented opcode in this project. */
public final class SetInterruptMode implements Z80Instruction {
	public static final SetInterruptMode MODE_0 = new SetInterruptMode(0);
	public static final SetInterruptMode MODE_1 = new SetInterruptMode(1);
	public static final SetInterruptMode MODE_2 = new SetInterruptMode(2);

	private final int mode;

	private SetInterruptMode(int mode) {
		this.mode = mode;
	}

	@Override
	public void execute(Z80CPU cpu) {
		cpu.interruptMode = mode;
	}
}
