package com.mj.z80;

/** One of BC/DE/HL/AF (plus their shadow-set counterparts) or IX/IY - a
 * single 16-bit value that's also addressable as two independent 8-bit
 * halves (e.g. INC BC vs INC B are different opcodes with different flag
 * effects). Backed by one int rather than two separate 8-bit fields that
 * something else would have to keep in sync with each other - hi()/lo()
 * and get16()/set16() all read and write the exact same storage. */
public class RegisterPair {
	private int value;

	public int get16() {
		return value & 0xffff;
	}

	public void set16(int v) {
		value = v & 0xffff;
	}

	/** The high byte - B of BC, D of DE, H of HL, A of AF. */
	public int hi() {
		return (value >> 8) & 0xff;
	}

	/** The low byte - C of BC, E of DE, L of HL, F (flags) of AF. */
	public int lo() {
		return value & 0xff;
	}

	public void setHi(int v) {
		value = ((v & 0xff) << 8) | (value & 0xff);
	}

	public void setLo(int v) {
		value = (value & 0xff00) | (v & 0xff);
	}
}
