package com.mj.Firmware.Math;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.mj.Firmware.Framework.OpCodes;
import com.mj.cpu001.CPU;
import com.mj.util.CPU_CreateUtil;

/**
 * ADC / SBC through every addressing mode. The arithmetic itself is verified exhaustively in
 * Test_AddSub_Exhaustive; this checks the operand fetch, that memory operands >= $80 are not
 * sign-extended, that memory is left alone, and the PC advance.
 */
public class Test_AddSub_Modes {
	private CPU c;

	@BeforeEach
	public void setup() throws Exception {
		c = CPU_CreateUtil.getCPU();
		c.step();               // consume power-on reset
	}

	private static final String[] MODES = { "IMM", "ZP", "ZPX", "ABS", "ABSX", "ABSY", "INX", "INY" };
	private static final int[] ADC_OPCODES = { 0x69, 0x65, 0x75, 0x6d, 0x7d, 0x79, 0x61, 0x71 };
	private static final int[] SBC_OPCODES = { 0xe9, 0xe5, 0xf5, 0xed, 0xfd, 0xf9, 0xe1, 0xf1 };
	private static final int[] OPERAND_BYTES = { 1, 1, 1, 2, 2, 2, 1, 1 };
	private static final int[] OPERANDS = { 0x00, 0x01, 0x7f, 0x80, 0xc8, 0xff };
	private static final int[] ACCUMULATORS = { 0x00, 0x50, 0x80, 0xff };

	private void a(int v) { try { c.a.set(v); } catch (Exception e) { } }
	private void x(int v) { try { c.x.set(v); } catch (Exception e) { } }
	private void y(int v) { try { c.y.set(v); } catch (Exception e) { } }

	/** Returns { operand bytes..., address holding the operand (or -1 for immediate) }. */
	private int[] arrange(String mode, int operand) throws Exception {
		switch (mode) {
		case "IMM": return new int[] { operand, -1 };
		case "ZP": c.bus.write(0x30, (byte) operand); return new int[] { 0x30, 0x30 };
		case "ZPX": x(0x10); c.bus.write(0x05, (byte) operand); return new int[] { 0xf5, 0x05 };      // wraps to $05
		case "ABS": c.bus.write(0x2345, (byte) operand); return new int[] { 0x45, 0x23, 0x2345 };
		case "ABSX": x(0x0b); c.bus.write(0x2350, (byte) operand); return new int[] { 0x45, 0x23, 0x2350 };
		case "ABSY": y(0x0b); c.bus.write(0x2350, (byte) operand); return new int[] { 0x45, 0x23, 0x2350 };
		case "INX": x(0x04); c.bus.write(0x24, (byte) 0x74); c.bus.write(0x25, (byte) 0x20);
			c.bus.write(0x2074, (byte) operand); return new int[] { 0x20, 0x2074 };
		default: y(0x10); c.bus.write(0x86, (byte) 0x28); c.bus.write(0x87, (byte) 0x40);
			c.bus.write(0x4038, (byte) operand); return new int[] { 0x86, 0x4038 };
		}
	}

	private void check(boolean sbc, int modeIndex, boolean decimal) throws Exception {
		String mode = MODES[modeIndex];
		OpCodes op = OpCodes.valueOf((sbc ? "SBC_" : "ADC_") + mode);
		assertEquals((sbc ? SBC_OPCODES : ADC_OPCODES)[modeIndex], op.code() & 0xff, op + " opcode");
		for (int operand : OPERANDS) {
			for (int acc : ACCUMULATORS) {
				for (int carry = 0; carry <= 1; carry++) {
					c.pc = 0x1000;
					int[] layout = arrange(mode, operand);
					a(acc);
					c.bus.write(0x1000, op.code());
					int nBytes = OPERAND_BYTES[modeIndex];
					for (int i = 0; i < nBytes; i++) c.bus.write(0x1001 + i, (byte) layout[i]);
					if (carry == 1) c.CFLAG.set(); else c.CFLAG.clear();
					if (decimal) c.DFLAG.set(); else c.DFLAG.clear();

					c.step();

					int[] exp = sbc ? AddSubModel.sbc(acc, operand, carry, decimal) : AddSubModel.adc(acc, operand, carry, decimal);
					String ctx = String.format("%s%s A=%02x M=%02x C=%d", op, decimal ? "(D)" : "", acc, operand, carry);
					assertEquals(exp[0], c.a.get(), ctx + " result");
					assertEquals(exp[1] == 1, c.CFLAG.isSet(), ctx + " carry");
					assertEquals(exp[2] == 1, c.NFLAG.isSet(), ctx + " negative");
					assertEquals(exp[3] == 1, c.OFLAG.isSet(), ctx + " overflow");
					assertEquals(exp[4] == 1, c.ZFLAG.isSet(), ctx + " zero");
					assertEquals(0x1001 + nBytes, c.pc, ctx + " pc");
					int operandAddress = layout[nBytes];
					if (operandAddress >= 0) {
						assertEquals(operand, c.bus.read(operandAddress) & 0xff, ctx + " memory operand unchanged");
					}
				}
			}
		}
	}

	@Test public void adcBinaryAllModes() throws Exception { for (int m = 0; m < MODES.length; m++) check(false, m, false); }
	@Test public void adcDecimalAllModes() throws Exception { for (int m = 0; m < MODES.length; m++) check(false, m, true); }
	@Test public void sbcBinaryAllModes() throws Exception { for (int m = 0; m < MODES.length; m++) check(true, m, false); }
	@Test public void sbcDecimalAllModes() throws Exception { for (int m = 0; m < MODES.length; m++) check(true, m, true); }

	/** Legacy: every memory mode sign-extended the operand, so $80 + $80 gave C=0, V=0. */
	@Test
	public void highMemoryOperandIsNotSignExtended() throws Exception {
		c.bus.write(0x30, (byte) 0x80);
		a(0x80);
		c.pc = 0x1000;
		c.bus.write(0x1000, OpCodes.ADC_ZP.code());
		c.bus.write(0x1001, (byte) 0x30);
		c.CFLAG.clear(); c.DFLAG.clear();
		c.step();
		assertEquals(0x00, c.a.get());
		assertEquals(true, c.CFLAG.isSet(), "$80 + $80 carries out");
		assertEquals(true, c.OFLAG.isSet(), "-128 + -128 overflows");
	}

	/** Legacy: ADC abs,X and abs,Y advanced the PC by only 1. */
	@Test
	public void adcAbsoluteIndexedAdvancesPcByThree() throws Exception {
		x(1); y(1);
		for (OpCodes op : new OpCodes[] { OpCodes.ADC_ABSX, OpCodes.ADC_ABSY }) {
			c.pc = 0x1000;
			c.bus.write(0x1000, op.code());
			c.bus.write(0x1001, (byte) 0x00);
			c.bus.write(0x1002, (byte) 0x20);
			c.step();
			assertEquals(0x1003, c.pc, op.toString());
		}
	}
}
