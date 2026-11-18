package com.mj.z80.instructions;

import java.util.function.ObjIntConsumer;
import java.util.function.ToIntFunction;

import com.mj.z80.Z80CPU;
import com.mj.z80.Z80Instruction;

/** INC BC/DE/HL/SP ($03-$33) and INC IX/IY ($23 under DD/FD) - a 16-bit
 * increment that affects NO flag at all, a real hardware quirk distinct
 * from the 8-bit INC family (see IncDecOperations). Takes a plain getter/
 * setter rather than RegisterPair16/IndexRegister directly, so SP (a
 * plain int field, not a RegisterPair) and IX/IY (RegisterPairs reached
 * through IndexRegister) can share this one class - same reasoning as
 * LoadRegisterPairFromMemory/StoreRegisterPairToMemory. */
public final class IncrementRegisterPair implements Z80Instruction {
	private final ToIntFunction<Z80CPU> getter;
	private final ObjIntConsumer<Z80CPU> setter;

	public IncrementRegisterPair(ToIntFunction<Z80CPU> getter, ObjIntConsumer<Z80CPU> setter) {
		this.getter = getter;
		this.setter = setter;
	}

	@Override
	public void execute(Z80CPU cpu) {
		setter.accept(cpu, (getter.applyAsInt(cpu) + 1) & 0xffff);
	}
}
