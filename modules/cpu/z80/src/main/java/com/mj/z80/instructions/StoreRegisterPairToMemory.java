package com.mj.z80.instructions;

import java.util.function.ToIntFunction;

import com.mj.exceptions.DeviceUnavailable;
import com.mj.exceptions.ROException;
import com.mj.exceptions.illegalAddressException;
import com.mj.z80.Z80CPU;
import com.mj.z80.Z80Instruction;

/** LD (nn),HL / LD (nn),IX / LD (nn),IY and the ED-prefixed LD (nn),dd
 * family - the store-direction counterpart of LoadRegisterPairFromMemory;
 * see its class comment for why this takes a plain getter rather than a
 * RegisterPair16/IndexRegister directly (SP again). Never affects any
 * flag. */
public final class StoreRegisterPairToMemory implements Z80Instruction {
	private final ToIntFunction<Z80CPU> getter;

	public StoreRegisterPairToMemory(ToIntFunction<Z80CPU> getter) {
		this.getter = getter;
	}

	@Override
	public void execute(Z80CPU cpu) throws illegalAddressException, DeviceUnavailable, ROException {
		int address = (cpu.fetchByte() & 0xff) | ((cpu.fetchByte() & 0xff) << 8);
		int value = getter.applyAsInt(cpu);
		cpu.memoryBus.write(address, (byte) (value & 0xff));
		cpu.memoryBus.write((address + 1) & 0xffff, (byte) ((value >> 8) & 0xff));
	}
}
