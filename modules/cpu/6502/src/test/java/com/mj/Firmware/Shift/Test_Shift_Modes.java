package com.mj.Firmware.Shift;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.mj.Firmware.Framework.OpCodes;
import com.mj.cpu001.CPU;
import com.mj.util.CPU_CreateUtil;

/**
 * ASL / LSR / ROL / ROR in all five addressing modes, driven through an exhaustive
 * edge-value matrix and checked against an independent model of the operation.
 */
public class Test_Shift_Modes {
	private CPU c;

	@BeforeEach
	public void setup() throws Exception {
		c = CPU_CreateUtil.getCPU();
		c.step();               // consume power-on reset
		c.pc = 0x1000;
	}

	private static final String[] OPS = { "ASL", "LSR", "ROL", "ROR" };
	private static final String[] MODES = { "ACC", "ZP", "ZPX", "ABS", "ABX" };
	/** Enum constant for each (op, mode); the legacy names are not uniform. */
	private static final String[][] NAMES = {
			{ "ASL", "ASL_ZP", "ASL_ZPX", "ASL_ABS", "ASL_ABSX" },
			{ "LSR_ACC", "LSR_ZP", "LSR_ZPX", "LSR_ABS", "LSR_ABX" },
			{ "ROL_ACC", "ROL_ZP", "ROL_ZPX", "ROL_ABS", "ROL_ABX" },
			{ "ROR_ACC", "ROR_ZP", "ROR_ZPX", "ROR_ABS", "ROR_ABX" } };
	private static final int[][] OPCODES = {
			{ 0x0a, 0x06, 0x16, 0x0e, 0x1e },
			{ 0x4a, 0x46, 0x56, 0x4e, 0x5e },
			{ 0x2a, 0x26, 0x36, 0x2e, 0x3e },
			{ 0x6a, 0x66, 0x76, 0x6e, 0x7e } };
	private static final int[] OPERAND_BYTES = { 0, 1, 1, 2, 2 };
	private static final int[] VALUES = { 0x00, 0x01, 0x02, 0x40, 0x55, 0x7f, 0x80, 0x81, 0xaa, 0xfe, 0xff };

	/** Independent model: returns { result, carryOut }. */
	private static int[] model(int op, int v, int carryIn) {
		switch (op) {
		case 0: return new int[] { (v << 1) & 0xff, (v >> 7) & 1 };                       // ASL
		case 1: return new int[] { v >> 1, v & 1 };                                          // LSR
		case 2: return new int[] { ((v << 1) | carryIn) & 0xff, (v >> 7) & 1 };            // ROL
		default: return new int[] { (v >> 1) | (carryIn << 7), v & 1 };                    // ROR
		}
	}

	private void a(int v) { try { c.a.set(v); } catch (Exception e) { } }
	private void x(int v) { try { c.x.set(v); } catch (Exception e) { } }

	/** Place the operand for a mode; returns the address that holds the value (or -1 for the accumulator). */
	private int arrange(int mode, int value, int[] operandsOut) throws Exception {
		switch (mode) {
		case 0: a(value); return -1;
		case 1: operandsOut[0] = 0x30; c.bus.write(0x30, (byte) value); return 0x30;
		case 2: x(0x10); operandsOut[0] = 0xf5; c.bus.write(0x05, (byte) value); return 0x05;    // wraps in page 0
		case 3: operandsOut[0] = 0x45; operandsOut[1] = 0x23; c.bus.write(0x2345, (byte) value); return 0x2345;
		default: x(0x0b); operandsOut[0] = 0x45; operandsOut[1] = 0x23; c.bus.write(0x2350, (byte) value); return 0x2350;
		}
	}

	@Test
	public void opcodesMatchTheRealInstructionSet() {
		for (int o = 0; o < OPS.length; o++) {
			for (int m = 0; m < MODES.length; m++) {
				assertEquals(OPCODES[o][m], OpCodes.valueOf(NAMES[o][m]).code() & 0xff, NAMES[o][m]);
			}
		}
	}

	@Test
	public void edgeValueMatrixInEveryMode() throws Exception {
		for (int o = 0; o < OPS.length; o++) {
			for (int m = 0; m < MODES.length; m++) {
				for (int value : VALUES) {
					for (int carryIn = 0; carryIn <= 1; carryIn++) {
						setup();
						OpCodes op = OpCodes.valueOf(NAMES[o][m]);
						int[] operands = new int[2];
						int addr = arrange(m, value, operands);
						c.bus.write(0x1000, op.code());
						for (int i = 0; i < OPERAND_BYTES[m]; i++) c.bus.write(0x1001 + i, (byte) operands[i]);
						if (carryIn == 1) c.CFLAG.set(); else c.CFLAG.clear();
						c.OFLAG.set();                         // must survive
						c.DFLAG.set();                         // must survive
						// preload the accumulator in memory modes so we can see it is left alone
						if (m != 0) a(0x99);

						c.step();

						int[] exp = model(o, value, carryIn);
						String ctx = String.format("%s value=%02x carryIn=%d", NAMES[o][m], value, carryIn);
						int actual = (addr < 0) ? c.a.get() : (c.bus.read(addr) & 0xff);
						assertEquals(exp[0], actual, ctx + " result");
						assertEquals(exp[1] == 1, c.CFLAG.isSet(), ctx + " carry");
						assertEquals(exp[0] == 0, c.ZFLAG.isSet(), ctx + " zero");
						assertEquals((exp[0] & 0x80) != 0, c.NFLAG.isSet(), ctx + " negative");
						assertEquals(true, c.OFLAG.isSet(), ctx + " overflow untouched");
						assertEquals(true, c.DFLAG.isSet(), ctx + " decimal untouched");
						assertEquals(0x1001 + OPERAND_BYTES[m], c.pc, ctx + " pc");
						if (m != 0) assertEquals(0x99, c.a.get(), ctx + " accumulator untouched by memory form");
					}
				}
			}
		}
	}

	@Test
	public void carryOutOfOneOpDoesNotLeakIntoTheNext() throws Exception {
		// ASL of 0x80 sets C; a following LSR of 0x02 must clear it again.
		a(0x80);
		c.bus.write(0x1000, OpCodes.valueOf("ASL").code());
		c.bus.write(0x1001, OpCodes.valueOf("LSR_ACC").code());
		c.step();
		assertEquals(true, c.CFLAG.isSet());
		assertEquals(0x00, c.a.get());
		a(0x02);
		c.step();
		assertEquals(false, c.CFLAG.isSet());
		assertEquals(0x01, c.a.get());
	}
}
