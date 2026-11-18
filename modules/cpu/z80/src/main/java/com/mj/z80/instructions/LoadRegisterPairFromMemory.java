package com.mj.z80.instructions;

import java.util.function.ObjIntConsumer;

import com.mj.exceptions.DeviceUnavailable;
import com.mj.exceptions.illegalAddressException;
import com.mj.z80.Z80CPU;
import com.mj.z80.Z80Instruction;

/** LD HL,(nn) / LD IX,(nn) / LD IY,(nn) (unprefixed/DD/FD) and the
 * ED-prefixed LD dd,(nn) family (BC/DE/HL/SP) - loads a 16-bit register
 * from a direct memory address (low byte then high byte, both the
 * address itself and the value read back), the address following the
 * opcode in the instruction stream. Takes a plain (Z80CPU, int) setter
 * rather than a RegisterPair16/IndexRegister directly - SP (a plain int
 * field, not a RegisterPair) needs to be assignable here too (LD SP,(nn)
 * is real), and a setter is the one shape all of RegisterPair16.set16(),
 * IndexRegister's RegisterPair.set16(), and a direct `cpu.sp = v` lambda
 * already share. Never affects any flag. */
public final class LoadRegisterPairFromMemory implements Z80Instruction {
	private final ObjIntConsumer<Z80CPU> setter;

	public LoadRegisterPairFromMemory(ObjIntConsumer<Z80CPU> setter) {
		this.setter = setter;
	}

	@Override
	public void execute(Z80CPU cpu) throws illegalAddressException, DeviceUnavailable {
		int address = (cpu.fetchByte() & 0xff) | ((cpu.fetchByte() & 0xff) << 8);
		int low = cpu.memoryBus.read(address) & 0xff;
		int high = cpu.memoryBus.read((address + 1) & 0xffff) & 0xff;
		setter.accept(cpu, low | (high << 8));
	}
}
