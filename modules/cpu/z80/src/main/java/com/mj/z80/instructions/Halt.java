package com.mj.z80.instructions;

import com.mj.z80.Z80CPU;
import com.mj.z80.Z80Instruction;

/** HALT ($76) - carved out of LoadRegisterTable's own `01 ddd sss` loop
 * (see its own comment: ddd=sss=110 would mean "LD (HL),(HL)", not a real
 * opcode). Real hardware repeats an internal NOP, PC parked right where it
 * is, until an NMI or an accepted maskable interrupt arrives - modeled here
 * as a flag Z80CPU.step() checks and simply does nothing while set, rather
 * than actually re-decoding/re-fetching every cycle. PC needs no special
 * adjustment on wake (unlike some descriptions of real hardware, which
 * describe PC as needing to be bumped past HALT on interrupt entry): this
 * emulator's fetchByte() already advanced PC past this opcode the moment it
 * was read, same as every other instruction, so Z80CPU.serviceInterrupts()
 * just needs to clear the flag - see its own comment. */
public final class Halt implements Z80Instruction {
	public static final Halt INSTANCE = new Halt();

	private Halt() {
	}

	@Override
	public void execute(Z80CPU cpu) {
		cpu.halted = true;
	}
}
