package com.mj.Firmware.Math;

/**
 * Independent reference model of the NMOS 6502 ADC and SBC, written from Bruce Clark's
 * "Decimal Mode" appendix (http://www.6502.org/tutorials/decimal_mode.html). It deliberately uses
 * his signed-arithmetic formulation for V, not the bit formula used by the implementation.
 * Result: { A, C, N, V, Z }.
 */
final class AddSubModel {
	private AddSubModel() {
	}

	static int[] adc(int a, int b, int carry, boolean decimal) {
		int binary = a + b + carry;
		int z = ((binary & 0xff) == 0) ? 1 : 0;
		if (!decimal) {
			int res = binary & 0xff;
			int signed = (byte) a + (byte) b + carry;
			return new int[] { res, binary > 0xff ? 1 : 0, (res & 0x80) != 0 ? 1 : 0,
					(signed < -128 || signed > 127) ? 1 : 0, z };
		}
		// Sequence 1: accumulator and carry
		int al = (a & 0x0f) + (b & 0x0f) + carry;
		if (al >= 0x0a) {
			al = ((al + 0x06) & 0x0f) + 0x10;
		}
		int t = (a & 0xf0) + (b & 0xf0) + al;
		if (t >= 0xa0) {
			t += 0x60;
		}
		// Sequence 2: N and V, using signed arithmetic
		int s = (byte) (a & 0xf0) + (byte) (b & 0xf0) + al;
		return new int[] { t & 0xff, t >= 0x100 ? 1 : 0, (s & 0x80) != 0 ? 1 : 0, (s < -128 || s > 127) ? 1 : 0, z };
	}

	static int[] sbc(int a, int b, int carry, boolean decimal) {
		// flags are the binary ones in both modes
		int diff = a - b - (1 - carry);
		int res = diff & 0xff;
		int signed = (byte) a - (byte) b - (1 - carry);
		int c = diff >= 0 ? 1 : 0;
		int n = (res & 0x80) != 0 ? 1 : 0;
		int v = (signed < -128 || signed > 127) ? 1 : 0;
		int z = res == 0 ? 1 : 0;
		if (!decimal) {
			return new int[] { res, c, n, v, z };
		}
		int al = (a & 0x0f) - (b & 0x0f) + carry - 1;
		if (al < 0) {
			al = ((al - 0x06) & 0x0f) - 0x10;
		}
		int t = (a & 0xf0) - (b & 0xf0) + al;
		if (t < 0) {
			t -= 0x60;
		}
		return new int[] { t & 0xff, c, n, v, z };
	}
}
