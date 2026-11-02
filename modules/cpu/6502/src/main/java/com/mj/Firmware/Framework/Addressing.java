package com.mj.Firmware.Framework;

import com.mj.cpu001.CPU;
import com.mj.exceptions.DeviceUnavailable;
import com.mj.exceptions.illegalAddressException;

/**
 * Effective-address calculation for each 6502 addressing mode. On entry the
 * PC points at the first operand byte (the opcode has already been fetched).
 * None of these advance the PC.
 */
public final class Addressing {
	private Addressing() {
	}

	/** Immediate: the operand is the byte at the PC. */
	public static int immediate(CPU c) {
		return c.pc;
	}

	public static int zeroPage(CPU c) throws illegalAddressException, DeviceUnavailable {
		return c.bus.read(c.pc) & 0xff;
	}

	public static int zeroPageX(CPU c) throws illegalAddressException, DeviceUnavailable {
		return (c.bus.read(c.pc) + c.x.get()) & 0xff;
	}

	public static int zeroPageY(CPU c) throws illegalAddressException, DeviceUnavailable {
		return (c.bus.read(c.pc) + c.y.get()) & 0xff;
	}

	public static int absolute(CPU c) throws illegalAddressException, DeviceUnavailable {
		int lower = c.bus.read(c.pc) & 0xff;
		int upper = c.bus.read((c.pc + 1) & 0xffff) & 0xff;
		return (upper << 8) | lower;
	}

	public static int absoluteX(CPU c) throws illegalAddressException, DeviceUnavailable {
		return (absolute(c) + (c.x.get() & 0xff)) & 0xffff;
	}

	public static int absoluteY(CPU c) throws illegalAddressException, DeviceUnavailable {
		return (absolute(c) + (c.y.get() & 0xff)) & 0xffff;
	}

	/** (zp,X): X is added to the zero-page pointer address; the pointer wraps in page 0. */
	public static int indexedIndirect(CPU c) throws illegalAddressException, DeviceUnavailable {
		int pointer = ((c.bus.read(c.pc) & 0xff) + c.x.get()) & 0xff;
		int lower = c.bus.read(pointer) & 0xff;
		int upper = c.bus.read((pointer + 1) & 0xff) & 0xff;
		return (upper << 8) | lower;
	}

	/** (zp),Y: Y is added to the address read from the zero-page pointer. */
	public static int indirectIndexed(CPU c) throws illegalAddressException, DeviceUnavailable {
		int pointer = c.bus.read(c.pc) & 0xff;
		int lower = c.bus.read(pointer) & 0xff;
		int upper = c.bus.read((pointer + 1) & 0xff) & 0xff;   // pointer wraps within zero page
		return (((upper << 8) | lower) + (c.y.get() & 0xff)) & 0xffff;
	}

	/** JMP (addr): reproduces the NMOS bug where the high byte never crosses a page. */
	public static int indirect(CPU c) throws illegalAddressException, DeviceUnavailable {
		int pointer = absolute(c);
		int highAddress = (pointer & 0xff00) | ((pointer + 1) & 0x00ff);
		int lower = c.bus.read(pointer) & 0xff;
		int upper = c.bus.read(highAddress) & 0xff;
		return (upper << 8) | lower;
	}

	/** True when adding an index moved the address into a different page. */
	public static boolean pageCrossed(int base, int effective) {
		return (base & 0xff00) != (effective & 0xff00);
	}
}
