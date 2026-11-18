package com.mj.z80.instructions;

import java.util.function.Function;

import com.mj.exceptions.DeviceUnavailable;
import com.mj.exceptions.ROException;
import com.mj.exceptions.illegalAddressException;
import com.mj.z80.RegisterPair;
import com.mj.z80.Z80CPU;
import com.mj.z80.Z80Instruction;

/** EX (SP),HL / EX (SP),IX / EX (SP),IY ($E3 unprefixed/DD/FD) - swaps
 * the register with the 16-bit value sitting on top of the stack (the
 * two bytes at SP/SP+1), leaving SP itself unchanged - a real memory
 * access, unlike JP (HL)/(IX)/(IY)'s own deceptive parens. Takes a plain
 * Function&lt;Z80CPU, RegisterPair&gt;, same as JumpToRegister/Push/Pop,
 * so one class covers HL/IX/IY. Affects no flags. */
public final class ExchangeStackTop implements Z80Instruction {
	private final Function<Z80CPU, RegisterPair> register;

	public ExchangeStackTop(Function<Z80CPU, RegisterPair> register) {
		this.register = register;
	}

	@Override
	public void execute(Z80CPU cpu) throws illegalAddressException, DeviceUnavailable, ROException {
		RegisterPair pair = register.apply(cpu);
		int low = cpu.memoryBus.read(cpu.sp) & 0xff;
		int high = cpu.memoryBus.read((cpu.sp + 1) & 0xffff) & 0xff;
		int stackTop = low | (high << 8);

		int registerValue = pair.get16();
		cpu.memoryBus.write(cpu.sp, (byte) (registerValue & 0xff));
		cpu.memoryBus.write((cpu.sp + 1) & 0xffff, (byte) ((registerValue >> 8) & 0xff));

		pair.set16(stackTop);
	}
}
