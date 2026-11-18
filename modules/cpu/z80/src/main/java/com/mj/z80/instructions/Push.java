package com.mj.z80.instructions;

import java.util.function.Function;

import com.mj.exceptions.DeviceUnavailable;
import com.mj.exceptions.ROException;
import com.mj.exceptions.illegalAddressException;
import com.mj.z80.RegisterPair;
import com.mj.z80.Z80CPU;
import com.mj.z80.Z80Instruction;

/** PUSH qq / PUSH IX / PUSH IY - decrements SP by 2 and writes a 16-bit
 * register pair to the stack, high byte first (at the higher of the two
 * final addresses), low byte second (at SP's own final value) - real Z80
 * hardware's own byte order, matching how Pop reads them back. Takes a
 * plain Function<Z80CPU, RegisterPair> rather than StackRegisterPair or
 * IndexRegister directly - both already expose exactly that shape
 * (StackRegisterPair::get, IndexRegister::get), and PUSH genuinely never
 * needs to reach SP as an operand (unlike the 16-bit LD memory family),
 * so there's no need for the ObjIntConsumer-style workaround that needed.
 * Never affects any flag - including PUSH AF, which just moves F's raw
 * bits around like any other register pair's low byte, with no
 * flag-specific behavior of its own. */
public final class Push implements Z80Instruction {
	private final Function<Z80CPU, RegisterPair> source;

	public Push(Function<Z80CPU, RegisterPair> source) {
		this.source = source;
	}

	@Override
	public void execute(Z80CPU cpu) throws illegalAddressException, DeviceUnavailable, ROException {
		int value = source.apply(cpu).get16();
		cpu.sp = (cpu.sp - 1) & 0xffff;
		cpu.memoryBus.write(cpu.sp, (byte) ((value >> 8) & 0xff));
		cpu.sp = (cpu.sp - 1) & 0xffff;
		cpu.memoryBus.write(cpu.sp, (byte) (value & 0xff));
	}
}
