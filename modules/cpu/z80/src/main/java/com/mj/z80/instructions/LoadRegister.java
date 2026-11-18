package com.mj.z80.instructions;

import com.mj.z80.Register8;
import com.mj.z80.Z80CPU;
import com.mj.z80.Z80Instruction;

/** LD r,r' - copies one 8-bit register into another. Covers every
 * register-to-register combination in the Z80's `01 ddd sss` opcode shape
 * (destination/source each one of B,C,D,E,H,L,A - never (HL), which is a
 * memory access, not a register, and gets its own instruction shape once
 * that's needed), parameterized rather than one class per combination,
 * same reasoning as the DD/FD "one builder, two closures" approach. Never
 * affects any flag - true of every 8-bit LD on real Z80 hardware, not
 * just this one. */
public final class LoadRegister implements Z80Instruction {
	private final Register8 destination;
	private final Register8 source;

	public LoadRegister(Register8 destination, Register8 source) {
		this.destination = destination;
		this.source = source;
	}

	@Override
	public void execute(Z80CPU cpu) {
		destination.set(cpu, source.get(cpu));
	}
}
