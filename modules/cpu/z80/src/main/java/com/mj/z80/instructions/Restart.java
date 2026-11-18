package com.mj.z80.instructions;

import com.mj.exceptions.DeviceUnavailable;
import com.mj.exceptions.ROException;
import com.mj.exceptions.illegalAddressException;
import com.mj.z80.Z80CPU;
import com.mj.z80.Z80Instruction;

/** RST p - a fast, one-byte CALL to one of eight fixed low-memory
 * addresses (0, 8, 16, ..., 56 - traditionally used for interrupt
 * handlers/syscall-style entry points, same role software interrupts
 * play on other architectures). Pushes the return address (PC, already
 * pointing past this single-byte instruction) exactly like CALL, just to
 * a fixed target instead of one read from the instruction stream. */
public final class Restart implements Z80Instruction {
	private final int address;

	public Restart(int address) {
		this.address = address;
	}

	@Override
	public void execute(Z80CPU cpu) throws illegalAddressException, DeviceUnavailable, ROException {
		StackHelper.push16(cpu, cpu.pc);
		cpu.pc = address;
	}
}
