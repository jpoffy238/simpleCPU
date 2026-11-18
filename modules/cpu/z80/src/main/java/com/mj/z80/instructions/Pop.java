package com.mj.z80.instructions;

import java.util.function.Function;

import com.mj.exceptions.DeviceUnavailable;
import com.mj.exceptions.illegalAddressException;
import com.mj.z80.RegisterPair;
import com.mj.z80.Z80CPU;
import com.mj.z80.Z80Instruction;

/** POP qq / POP IX / POP IY - reads a 16-bit register pair back off the
 * stack (low byte first, from SP's current value, then high byte, then
 * SP is incremented by 2 total) - the exact inverse of Push. Never
 * affects any flag, including POP AF - it just overwrites F with
 * whatever raw bits were saved there, same as loading any other register
 * pair's low byte; that's not POP computing a flag, just POP doing what
 * it always does. */
public final class Pop implements Z80Instruction {
	private final Function<Z80CPU, RegisterPair> destination;

	public Pop(Function<Z80CPU, RegisterPair> destination) {
		this.destination = destination;
	}

	@Override
	public void execute(Z80CPU cpu) throws illegalAddressException, DeviceUnavailable {
		int low = cpu.memoryBus.read(cpu.sp) & 0xff;
		cpu.sp = (cpu.sp + 1) & 0xffff;
		int high = cpu.memoryBus.read(cpu.sp) & 0xff;
		cpu.sp = (cpu.sp + 1) & 0xffff;
		destination.apply(cpu).set16(low | (high << 8));
	}
}
