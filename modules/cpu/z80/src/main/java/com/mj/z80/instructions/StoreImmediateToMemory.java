package com.mj.z80.instructions;

import com.mj.exceptions.DeviceUnavailable;
import com.mj.exceptions.ROException;
import com.mj.exceptions.illegalAddressException;
import com.mj.z80.Z80CPU;
import com.mj.z80.Z80Instruction;

/** LD (HL),n ($36) - writes a literal byte, following the opcode in the
 * instruction stream, to the address HL points at. The one immediate-to-
 * memory form in the plain-HL LD family - no register parameterization
 * needed (unlike LoadFromMemory/StoreToMemory), since (HL) is the only
 * possible destination and there's no register operand at all, just the
 * literal. Stateless, so one shared instance covers every call, same as
 * NOP. Never affects any flag, same as every other LD. */
public final class StoreImmediateToMemory implements Z80Instruction {
	public static final StoreImmediateToMemory INSTANCE = new StoreImmediateToMemory();

	private StoreImmediateToMemory() {
	}

	@Override
	public void execute(Z80CPU cpu) throws illegalAddressException, DeviceUnavailable, ROException {
		byte value = cpu.fetchByte();
		cpu.memoryBus.write(cpu.hl.get16(), value);
	}
}
