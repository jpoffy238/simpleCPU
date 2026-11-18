package com.mj.z80.instructions;

import com.mj.exceptions.DeviceUnavailable;
import com.mj.exceptions.ROException;
import com.mj.exceptions.illegalAddressException;
import com.mj.z80.Flags;
import com.mj.z80.Z80CPU;

/** Shared math for RLD/RRD - both treat A's low nibble and (HL)'s two
 * nibbles as one 12-bit chain and rotate it by one nibble through
 * memory, in opposite directions. Flags (shared by both directions): S/Z
 * from the new A, P/V its parity (same convention as the logic ALU
 * family and the CB-prefixed shift family), H=0, N=0, C preserved. */
final class DigitRotateOperations {
	private DigitRotateOperations() {
	}

	/** RLD (ED $6F): new(HL)_hi = old(HL)_lo; new(HL)_lo = oldA_lo;
	 * newA_lo = old(HL)_hi. A's high nibble is unaffected. */
	static void rotateLeft(Z80CPU cpu) throws illegalAddressException, DeviceUnavailable, ROException {
		int hl = cpu.hl.get16();
		int memoryValue = cpu.memoryBus.read(hl) & 0xff;
		int a = cpu.af.hi();

		int newMemoryValue = ((memoryValue << 4) | (a & 0x0f)) & 0xff;
		int newA = (a & 0xf0) | (memoryValue >> 4);

		cpu.memoryBus.write(hl, (byte) newMemoryValue);
		cpu.af.setHi(newA);
		setFlags(cpu, newA);
	}

	/** RRD (ED $67): new(HL)_lo = old(HL)_hi; new(HL)_hi = oldA_lo;
	 * newA_lo = old(HL)_lo. A's high nibble is unaffected. */
	static void rotateRight(Z80CPU cpu) throws illegalAddressException, DeviceUnavailable, ROException {
		int hl = cpu.hl.get16();
		int memoryValue = cpu.memoryBus.read(hl) & 0xff;
		int a = cpu.af.hi();

		int newMemoryValue = ((memoryValue >> 4) | ((a & 0x0f) << 4)) & 0xff;
		int newA = (a & 0xf0) | (memoryValue & 0x0f);

		cpu.memoryBus.write(hl, (byte) newMemoryValue);
		cpu.af.setHi(newA);
		setFlags(cpu, newA);
	}

	private static void setFlags(Z80CPU cpu, int newA) {
		int flags = cpu.af.lo() & Flags.C;
		if ((newA & 0x80) != 0) {
			flags |= Flags.S;
		}
		if (newA == 0) {
			flags |= Flags.Z;
		}
		if (Integer.bitCount(newA) % 2 == 0) {
			flags |= Flags.PV;
		}
		cpu.af.setLo(flags);
	}
}
