package com.mj.Firmware.Table;

import com.mj.Firmware.Framework.Addressing;
import com.mj.cpu001.CPU;
import com.mj.exceptions.DeviceUnavailable;
import com.mj.exceptions.illegalAddressException;

/**
 * An addressing mode: how to find the operand, and how many operand bytes the
 * instruction has. The PC advance for every table-driven instruction comes
 * from {@link #operandBytes()}, so it exists in exactly one place.
 */
public enum AddrMode {
	IMMEDIATE("Immediate", 1, false, Addressing::immediate),
	ZERO_PAGE("Zero Page", 1, false, Addressing::zeroPage),
	ZERO_PAGE_X("Zero Page,X", 1, false, Addressing::zeroPageX),
	ZERO_PAGE_Y("Zero Page,Y", 1, false, Addressing::zeroPageY),
	ABSOLUTE("Absolute", 2, false, Addressing::absolute),
	ABSOLUTE_X("Absolute,X", 2, true, Addressing::absoluteX),
	ABSOLUTE_Y("Absolute,Y", 2, true, Addressing::absoluteY),
	INDEXED_INDIRECT("Indirect,X", 1, false, Addressing::indexedIndirect),
	INDIRECT_INDEXED("Indirect,Y", 1, true, Addressing::indirectIndexed),
	/** Operates on the accumulator; there is no memory operand. */
	ACCUMULATOR("Accumulator", 0, false, null),
	IMPLIED("Implied", 0, false, null);

	/** Computes the effective address. */
	@FunctionalInterface
	public interface Resolver {
		int resolve(CPU c) throws illegalAddressException, DeviceUnavailable;
	}

	private final String label;
	private final int operandBytes;
	private final boolean pageCrossPenalty;
	private final Resolver resolver;

	AddrMode(String label, int operandBytes, boolean pageCrossPenalty, Resolver resolver) {
		this.label = label;
		this.operandBytes = operandBytes;
		this.pageCrossPenalty = pageCrossPenalty;
		this.resolver = resolver;
	}

	public String label() {
		return label;
	}

	public int operandBytes() {
		return operandBytes;
	}

	/** Total instruction length in bytes, including the opcode. */
	public int size() {
		return 1 + operandBytes;
	}

	/** True if a read through this mode costs an extra cycle when it crosses a page. */
	public boolean hasPageCrossPenalty() {
		return pageCrossPenalty;
	}

	public boolean hasMemoryOperand() {
		return resolver != null;
	}

	public int address(CPU c) throws illegalAddressException, DeviceUnavailable {
		return resolver.resolve(c);
	}
}
