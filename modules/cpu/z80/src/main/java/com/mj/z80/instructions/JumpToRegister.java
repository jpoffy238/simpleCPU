package com.mj.z80.instructions;

import java.util.function.Function;

import com.mj.z80.RegisterPair;
import com.mj.z80.Z80CPU;
import com.mj.z80.Z80Instruction;

/** JP (HL) / JP (IX) / JP (IY) ($E9 unprefixed/DD/FD) - copies the
 * register's own 16-bit value directly into PC. The "(HL)" notation is a
 * real, well-known point of confusion: despite looking like a memory
 * reference, this instruction never touches memory at all - it's
 * functionally "LD PC,HL", not "jump to the address stored at [HL]" (that
 * would need two more bytes to read, and isn't what this opcode does on
 * real hardware). Takes a plain Function<Z80CPU, RegisterPair>, same as
 * Push/Pop, so the same class covers HL/IX/IY. */
public final class JumpToRegister implements Z80Instruction {
	private final Function<Z80CPU, RegisterPair> register;

	public JumpToRegister(Function<Z80CPU, RegisterPair> register) {
		this.register = register;
	}

	@Override
	public void execute(Z80CPU cpu) {
		cpu.pc = register.apply(cpu).get16();
	}
}
