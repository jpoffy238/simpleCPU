package com.mj.z80.instructions;

import java.util.function.ObjIntConsumer;
import java.util.function.ToIntFunction;

import com.mj.z80.Z80CPU;
import com.mj.z80.Z80Instruction;

/** DEC BC/DE/HL/SP ($0B-$3B) and DEC IX/IY ($2B under DD/FD) - see
 * IncrementRegisterPair's class comment; same no-flags-at-all rule and
 * same plain getter/setter shape, just subtracting one instead. */
public final class DecrementRegisterPair implements Z80Instruction {
	private final ToIntFunction<Z80CPU> getter;
	private final ObjIntConsumer<Z80CPU> setter;

	public DecrementRegisterPair(ToIntFunction<Z80CPU> getter, ObjIntConsumer<Z80CPU> setter) {
		this.getter = getter;
		this.setter = setter;
	}

	@Override
	public void execute(Z80CPU cpu) {
		setter.accept(cpu, (getter.applyAsInt(cpu) - 1) & 0xffff);
	}
}
