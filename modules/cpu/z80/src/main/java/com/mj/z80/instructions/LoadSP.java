package com.mj.z80.instructions;

import java.util.function.ToIntFunction;

import com.mj.z80.Z80CPU;
import com.mj.z80.Z80Instruction;

/** LD SP,HL / LD SP,IX / LD SP,IY ($F9 unprefixed/DD/FD) - copies a 16-bit
 * register directly into SP. Never affects any flag. */
public final class LoadSP implements Z80Instruction {
	private final ToIntFunction<Z80CPU> source;

	public LoadSP(ToIntFunction<Z80CPU> source) {
		this.source = source;
	}

	@Override
	public void execute(Z80CPU cpu) {
		cpu.sp = source.applyAsInt(cpu) & 0xffff;
	}
}
