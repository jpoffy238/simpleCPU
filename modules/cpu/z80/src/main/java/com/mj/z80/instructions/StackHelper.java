package com.mj.z80.instructions;

import com.mj.exceptions.DeviceUnavailable;
import com.mj.exceptions.ROException;
import com.mj.exceptions.illegalAddressException;
import com.mj.z80.Z80CPU;

/** Shared "decrement SP by 2 and write a 16-bit value" / "read one back
 * and increment SP by 2" logic - the same mechanical push/pop Push/Pop
 * perform for a named register pair, reused here by CALL/RST (push a
 * computed return address, not a register) and RET (pop straight into
 * PC). */
final class StackHelper {
	private StackHelper() {
	}

	static void push16(Z80CPU cpu, int value) throws illegalAddressException, DeviceUnavailable, ROException {
		cpu.sp = (cpu.sp - 1) & 0xffff;
		cpu.memoryBus.write(cpu.sp, (byte) ((value >> 8) & 0xff));
		cpu.sp = (cpu.sp - 1) & 0xffff;
		cpu.memoryBus.write(cpu.sp, (byte) (value & 0xff));
	}

	static int pop16(Z80CPU cpu) throws illegalAddressException, DeviceUnavailable {
		int low = cpu.memoryBus.read(cpu.sp) & 0xff;
		cpu.sp = (cpu.sp + 1) & 0xffff;
		int high = cpu.memoryBus.read(cpu.sp) & 0xff;
		cpu.sp = (cpu.sp + 1) & 0xffff;
		return low | (high << 8);
	}
}
