package com.mj.z80.instructions;

import com.mj.exceptions.DeviceUnavailable;
import com.mj.exceptions.illegalAddressException;
import com.mj.z80.Z80CPU;
import com.mj.z80.Z80Instruction;

/** LD A,(nn) ($3A) - loads A from a direct 16-bit memory address, the
 * address following the opcode in the instruction stream. A's own
 * addressing form - no other 8-bit register has an equivalent (B,C,...
 * only ever reach memory through (HL)/(IX+d)/(IY+d)). Stateless, so one
 * shared instance covers every call. Never affects any flag. */
public final class LoadAFromMemory implements Z80Instruction {
	public static final LoadAFromMemory INSTANCE = new LoadAFromMemory();

	private LoadAFromMemory() {
	}

	@Override
	public void execute(Z80CPU cpu) throws illegalAddressException, DeviceUnavailable {
		int address = (cpu.fetchByte() & 0xff) | ((cpu.fetchByte() & 0xff) << 8);
		cpu.af.setHi(cpu.memoryBus.read(address) & 0xff);
	}
}
