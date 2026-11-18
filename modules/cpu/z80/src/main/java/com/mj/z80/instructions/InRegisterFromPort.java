package com.mj.z80.instructions;

import com.mj.exceptions.DeviceUnavailable;
import com.mj.exceptions.illegalAddressException;
import com.mj.z80.Register8;
import com.mj.z80.Z80CPU;
import com.mj.z80.Z80Instruction;

/** IN r,(C) (ED $40 | r&lt;&lt;3) - reads the port addressed by the full BC
 * register into r, setting S/Z/P-V from the byte read (see
 * PortOperations). There's no r=6 slot - (HL) isn't a valid destination
 * here, and Register8 never has that code, so the slot is naturally left
 * unregistered by whatever builds this family's table (same mechanism as
 * every other family that loops over Register8.values()). */
public final class InRegisterFromPort implements Z80Instruction {
	private final Register8 destination;

	public InRegisterFromPort(Register8 destination) {
		this.destination = destination;
	}

	@Override
	public void execute(Z80CPU cpu) throws illegalAddressException, DeviceUnavailable {
		int port = cpu.bc.get16();
		int value = cpu.portBus.read(port) & 0xff;
		destination.set(cpu, value);
		PortOperations.setInputFlags(cpu, value);
	}
}
