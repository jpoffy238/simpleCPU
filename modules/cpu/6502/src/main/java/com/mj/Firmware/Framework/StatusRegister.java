package com.mj.Firmware.Framework;

import com.mj.cpu001.CPU;
import com.mj.Registers.registerFlags;

/**
 * The 6502 processor status byte:  N V 1 B D I Z C  (bit 7 .. bit 0).
 * B exists only in pushed copies of the status (1 for PHP/BRK, 0 for IRQ/NMI) and the
 * unused bit always reads as 1.
 */
public final class StatusRegister {
	private StatusRegister() {
	}

	public static final int C = 0x01;
	public static final int Z = 0x02;
	public static final int I = 0x04;
	public static final int D = 0x08;
	public static final int B = 0x10;
	public static final int UNUSED = 0x20;
	public static final int V = 0x40;
	public static final int N = 0x80;

	/** Build the status byte for pushing on the stack. */
	public static byte pack(CPU c, boolean breakFlag) {
		int status = UNUSED;
		if (c.CFLAG.isSet()) status |= C;
		if (c.ZFLAG.isSet()) status |= Z;
		if (c.IFLAG.isSet()) status |= I;
		if (c.DFLAG.isSet()) status |= D;
		if (breakFlag) status |= B;
		if (c.OFLAG.isSet()) status |= V;
		if (c.NFLAG.isSet()) status |= N;
		return (byte) status;
	}

	/** Load the flags from a status byte (PLP / RTI). B and the unused bit are ignored. */
	public static void load(CPU c, byte sr) {
		int v = sr & 0xff;
		set(c.CFLAG, (v & C) != 0);
		set(c.ZFLAG, (v & Z) != 0);
		set(c.IFLAG, (v & I) != 0);
		set(c.DFLAG, (v & D) != 0);
		set(c.OFLAG, (v & V) != 0);
		set(c.NFLAG, (v & N) != 0);
	}

	private static void set(registerFlags f, boolean on) {
		if (on) {
			f.set();
		} else {
			f.clear();
		}
	}
}
