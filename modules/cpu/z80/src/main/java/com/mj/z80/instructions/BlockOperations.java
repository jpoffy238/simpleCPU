package com.mj.z80.instructions;

import com.mj.exceptions.DeviceUnavailable;
import com.mj.exceptions.ROException;
import com.mj.exceptions.illegalAddressException;
import com.mj.z80.Flags;
import com.mj.z80.Z80CPU;

/** Shared math for the ED-prefixed block instructions - LDI/LDD/LDIR/
 * LDDR (block copy) and CPI/CPD/CPIR/CPDR (block compare). Both families
 * share the same "step HL (and DE, for the copy family) by +1 or -1,
 * decrement BC, set P/V from whether BC is still nonzero" shape, but
 * differ in what they actually do to memory each step and in which
 * flags beyond P/V they touch.
 *
 * The repeating forms (LDIR/LDDR/CPIR/CPDR - see BlockCopyRepeat/
 * BlockCompareRepeat) are implemented as a single Java loop inside one
 * execute() call, rather than real hardware's own trick of re-pointing
 * PC at its own 2-byte opcode and re-fetching/re-decoding it each pass -
 * this project doesn't model interrupts yet, so there's no observable
 * difference in the end state (memory, registers, flags) either way,
 * and a plain loop is far simpler. Revisit if interrupt delivery is ever
 * built and a block instruction needs to be interruptible mid-repeat on
 * real hardware's own terms. */
final class BlockOperations {
	private BlockOperations() {
	}

	/** One LDI/LDD step: (DE) = (HL), then HL and DE both step by
	 * direction (+1 or -1), then BC--. Sets H=0, N=0, P/V=(BC!=0 after
	 * the decrement); S, Z, and C are all left exactly as they were -
	 * this is a pure move, not a comparison or arithmetic op. */
	static void copyStep(Z80CPU cpu, int direction) throws illegalAddressException, DeviceUnavailable, ROException {
		int hl = cpu.hl.get16();
		int de = cpu.de.get16();
		byte value = cpu.memoryBus.read(hl);
		cpu.memoryBus.write(de, value);
		cpu.hl.set16((hl + direction) & 0xffff);
		cpu.de.set16((de + direction) & 0xffff);
		int bc = (cpu.bc.get16() - 1) & 0xffff;
		cpu.bc.set16(bc);

		int flags = cpu.af.lo() & (Flags.S | Flags.Z | Flags.C);
		if (bc != 0) {
			flags |= Flags.PV;
		}
		cpu.af.setLo(flags);
	}

	/** One CPI/CPD step: compares A with (HL) (same math as an ordinary
	 * CP - S/Z/H from the subtraction), then HL steps by direction, then
	 * BC--, P/V=(BC!=0 after the decrement), N=1. Unlike an ordinary CP,
	 * C is left exactly as it was - a real, documented quirk specific to
	 * this instruction. Returns true if a match was found (A equals the
	 * compared byte), for the repeating forms to know when to stop
	 * early. */
	static boolean compareStep(Z80CPU cpu, int direction) throws illegalAddressException, DeviceUnavailable {
		int hl = cpu.hl.get16();
		int a = cpu.af.hi();
		int operand = cpu.memoryBus.read(hl) & 0xff;
		int result = (a - operand) & 0xff;
		cpu.hl.set16((hl + direction) & 0xffff);
		int bc = (cpu.bc.get16() - 1) & 0xffff;
		cpu.bc.set16(bc);

		boolean halfCarry = (a & 0xf) - (operand & 0xf) < 0;

		int flags = cpu.af.lo() & Flags.C; // preserved, unlike an ordinary CP
		if ((result & 0x80) != 0) {
			flags |= Flags.S;
		}
		if (result == 0) {
			flags |= Flags.Z;
		}
		if (halfCarry) {
			flags |= Flags.H;
		}
		if (bc != 0) {
			flags |= Flags.PV;
		}
		flags |= Flags.N;
		cpu.af.setLo(flags);

		return result == 0;
	}
}
