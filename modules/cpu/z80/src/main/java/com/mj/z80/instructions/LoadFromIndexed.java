package com.mj.z80.instructions;

import com.mj.exceptions.DeviceUnavailable;
import com.mj.exceptions.illegalAddressException;
import com.mj.z80.IndexRegister;
import com.mj.z80.Register8;
import com.mj.z80.Z80CPU;
import com.mj.z80.Z80Instruction;

/** LD r,(IX+d) / LD r,(IY+d) - loads the byte at (index register + a
 * signed displacement) into one 8-bit register. The displacement is read
 * here, during execute(), the same as every other ordinary operand byte -
 * it's just a plain Java byte (signed, matching the real instruction's own
 * signed 8-bit range), so adding it to the index register's 16-bit value
 * sign-extends correctly before the result wraps back into 16 bits.
 * destination is a perfectly ordinary register here (even H or L) -
 * unlike the plain-HL family, there's no entanglement between the
 * register operand and the addressing: IX/IY only replaces HL in the
 * *addressing* role, never in h/l themselves. Never affects any flag. */
public final class LoadFromIndexed implements Z80Instruction {
	private final Register8 destination;
	private final IndexRegister index;

	public LoadFromIndexed(Register8 destination, IndexRegister index) {
		this.destination = destination;
		this.index = index;
	}

	@Override
	public void execute(Z80CPU cpu) throws illegalAddressException, DeviceUnavailable {
		byte displacement = cpu.fetchByte();
		int address = (index.get(cpu).get16() + displacement) & 0xffff;
		destination.set(cpu, cpu.memoryBus.read(address) & 0xff);
	}
}
