package com.mj.Firmware.Math;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.mj.Firmware.Framework.OpCodes;
import com.mj.Firmware.Table.Registers;
import com.mj.cpu001.CPU;
import com.mj.util.CPU_CreateUtil;

/**
 * ADC and SBC (immediate mode) for EVERY accumulator value, operand and carry-in, in binary and in
 * decimal mode: 4 x 131,072 = 524,288 cases, each checked against the independent model.
 */
public class Test_AddSub_Exhaustive {

	private void runExhaustive(OpCodes op, boolean sbc, boolean decimal) throws Exception {
		CPU c = CPU_CreateUtil.getCPU();
		c.step();                                 // consume power-on reset
		c.pc = 0x1001;                            // the operand byte; we execute the instruction directly
		if (decimal) c.DFLAG.set(); else c.DFLAG.clear();
		c.OFLAG.clear();
		List<String> failures = new ArrayList<>();
		int cases = 0;
		for (int m = 0; m < 256; m++) {
			c.bus.write(0x1001, (byte) m);
			for (int a = 0; a < 256; a++) {
				for (int carry = 0; carry <= 1; carry++) {
					Registers.put(c.a, a);
					if (carry == 1) c.CFLAG.set(); else c.CFLAG.clear();
					c.pc = 0x1001;
					op.microcode.exeute(c);
					int[] exp = sbc ? AddSubModel.sbc(a, m, carry, decimal) : AddSubModel.adc(a, m, carry, decimal);
					int[] act = { c.a.get(), c.CFLAG.isSet() ? 1 : 0, c.NFLAG.isSet() ? 1 : 0, c.OFLAG.isSet() ? 1 : 0,
							c.ZFLAG.isSet() ? 1 : 0 };
					cases++;
					if (!java.util.Arrays.equals(exp, act) && failures.size() < 8) {
						failures.add(String.format("%s%s A=%02x M=%02x C=%d  expected {A,C,N,V,Z}=%s  actual=%s",
								op, decimal ? "(D)" : "", a, m, carry, java.util.Arrays.toString(exp),
								java.util.Arrays.toString(act)));
					}
					assertEquals(0x1002, c.pc, "immediate operand is one byte");
					assertEquals(decimal, c.DFLAG.isSet(), "decimal flag must not change");
				}
			}
		}
		assertEquals(131072, cases);
		assertTrue(failures.isEmpty(), failures.size() + "+ mismatches, first:\n" + String.join("\n", failures));
	}

	@Test public void adcBinary() throws Exception { runExhaustive(OpCodes.ADC_IMM, false, false); }
	@Test public void adcDecimal() throws Exception { runExhaustive(OpCodes.ADC_IMM, false, true); }
	@Test public void sbcBinary() throws Exception { runExhaustive(OpCodes.SBC_IMM, true, false); }
	@Test public void sbcDecimal() throws Exception { runExhaustive(OpCodes.SBC_IMM, true, true); }
}
