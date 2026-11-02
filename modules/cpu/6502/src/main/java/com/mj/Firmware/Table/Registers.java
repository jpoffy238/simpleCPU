package com.mj.Firmware.Table;

import com.mj.Registers.generalPurpose;
import com.mj.cpu001.CPU;
import com.mj.exceptions.nflagException;
import com.mj.exceptions.zflagException;

/** Small helpers shared by table operations. */
public final class Registers {
	private Registers() {
	}

	/** Store a value in a register. Flags are set explicitly by the caller, so the register's Z/N signalling is ignored. */
	public static void put(generalPurpose register, int value) {
		try {
			register.set(value & 0xff);
		} catch (zflagException | nflagException e) {
			// intentionally ignored: see setNZ
		}
	}

	/** Set N and Z from an 8-bit result. */
	public static void setNZ(CPU c, int value) {
		value &= 0xff;
		if (value == 0) {
			c.ZFLAG.set();
		} else {
			c.ZFLAG.clear();
		}
		if ((value & 0x80) != 0) {
			c.NFLAG.set();
		} else {
			c.NFLAG.clear();
		}
	}

	public static void setCarry(CPU c, boolean on) {
		if (on) {
			c.CFLAG.set();
		} else {
			c.CFLAG.clear();
		}
	}
}
