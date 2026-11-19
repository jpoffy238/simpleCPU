package com.mj.z80.instructions;

import com.mj.exceptions.DeviceUnavailable;
import com.mj.exceptions.illegalAddressException;
import com.mj.z80.RegisterPair;
import com.mj.z80.Z80CPU;
import com.mj.z80.Z80Instruction;

/** LD A,(BC) ($0A) / LD A,(DE) ($1A) - loads A from the address held in BC
 * or DE. A's own addressing form, like LoadAFromMemory's direct (nn) - no
 * other 8-bit register reaches memory through BC/DE, only through
 * (HL)/(IX+d)/(IY+d). Never affects any flag.
 *
 * Takes a selector (cpu -> cpu.bc, not a captured RegisterPair) - table
 * population happens once, long before any particular CPU instance
 * exists, same reasoning IndexRegister's own class comment gives. */
public final class LoadAFromPairMemory implements Z80Instruction {
	@FunctionalInterface
	public interface Selector {
		RegisterPair get(Z80CPU cpu);
	}

	private final Selector source;

	public LoadAFromPairMemory(Selector source) {
		this.source = source;
	}

	@Override
	public void execute(Z80CPU cpu) throws illegalAddressException, DeviceUnavailable {
		cpu.af.setHi(cpu.memoryBus.read(source.get(cpu).get16()) & 0xff);
	}
}
