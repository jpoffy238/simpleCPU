package com.mj.z80.instructions;

import com.mj.z80.Flags;
import com.mj.z80.Z80CPU;

/** Shared rotate/shift math and flag computation for the CB-prefixed
 * RLC/RRC/RL/RR/SLA/SRA/SRL family (opcode group `00 ooo rrr`). All seven
 * share the same flag shape (S/Z/P-V from the result - yes, rotates set
 * parity too, not just the logic family; H=0; N=0; C from whichever bit
 * rotated/shifted out) even though each moves bits differently: RLC/RRC
 * wrap the outgoing bit back around to the empty side; RL/RR route it
 * through the existing carry flag instead; SLA/SRL shift a 0 into the
 * empty side; SRA alone preserves the sign bit (arithmetic shift) rather
 * than shifting a 0 in. SLL - the undocumented eighth variant real
 * hardware also implements, shifting a 1 into bit 0 - is deliberately
 * not here, same posture as every other undocumented opcode in this
 * project (see RotateShiftOperation). */
final class RotateShiftOperations {
	private RotateShiftOperations() {
	}

	static int rlc(Z80CPU cpu, int operand) {
		int carryOut = (operand >> 7) & 1;
		int result = ((operand << 1) | carryOut) & 0xff;
		setFlags(cpu, result, carryOut);
		return result;
	}

	static int rrc(Z80CPU cpu, int operand) {
		int carryOut = operand & 1;
		int result = ((operand >> 1) | (carryOut << 7)) & 0xff;
		setFlags(cpu, result, carryOut);
		return result;
	}

	static int rl(Z80CPU cpu, int operand) {
		int carryIn = (cpu.af.lo() & Flags.C) != 0 ? 1 : 0;
		int carryOut = (operand >> 7) & 1;
		int result = ((operand << 1) | carryIn) & 0xff;
		setFlags(cpu, result, carryOut);
		return result;
	}

	static int rr(Z80CPU cpu, int operand) {
		int carryIn = (cpu.af.lo() & Flags.C) != 0 ? 0x80 : 0;
		int carryOut = operand & 1;
		int result = ((operand >> 1) | carryIn) & 0xff;
		setFlags(cpu, result, carryOut);
		return result;
	}

	static int sla(Z80CPU cpu, int operand) {
		int carryOut = (operand >> 7) & 1;
		int result = (operand << 1) & 0xff;
		setFlags(cpu, result, carryOut);
		return result;
	}

	static int sra(Z80CPU cpu, int operand) {
		int carryOut = operand & 1;
		int result = ((operand >> 1) | (operand & 0x80)) & 0xff;
		setFlags(cpu, result, carryOut);
		return result;
	}

	static int srl(Z80CPU cpu, int operand) {
		int carryOut = operand & 1;
		int result = (operand >> 1) & 0xff;
		setFlags(cpu, result, carryOut);
		return result;
	}

	private static void setFlags(Z80CPU cpu, int result, int carryOut) {
		int flags = 0;
		if ((result & 0x80) != 0) {
			flags |= Flags.S;
		}
		if (result == 0) {
			flags |= Flags.Z;
		}
		if (Integer.bitCount(result & 0xff) % 2 == 0) {
			flags |= Flags.PV;
		}
		if (carryOut != 0) {
			flags |= Flags.C;
		}
		cpu.af.setLo(flags);
	}
}
