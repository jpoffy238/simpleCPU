package com.mj.Firmware.logic.modes;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.mj.Firmware.Framework.OpCodes;
import com.mj.cpu001.CPU;
import com.mj.util.CPU_CreateUtil;

/** AND / ORA / EOR in all eight addressing modes: result, flags, and PC advance. */
public class Test_Logical_Modes {
	private CPU c;

	@BeforeEach
	public void setup() throws Exception {
		c = CPU_CreateUtil.getCPU();
		c.step();               // consume power-on reset
		c.pc = 0x1000;
	}

	private void a(int v) { try { c.a.set(v); } catch (Exception e) { } }
	private void x(int v) { try { c.x.set(v); } catch (Exception e) { } }
	private void y(int v) { try { c.y.set(v); } catch (Exception e) { } }
	private void mem(int a, int v) throws Exception { c.bus.write(a, (byte) v); }

	private static final String[] MNEMONICS = { "AND", "ORA", "EOR" };
	private static final int[] EXPECTED = { 0x0c, 0xaf, 0xa3 };           // 0xAC op 0x0F

	/** Opcode name for a mode; the AND enum constants use ABX/ABY, the others ABSX/ABSY. */
	private static String name(String mnemonic, String mode) {
		if (mnemonic.equals("AND") && (mode.equals("ABSX") || mode.equals("ABSY"))) {
			return "AND_" + mode.substring(0, 2) + mode.charAt(3);
		}
		return mnemonic + "_" + mode;
	}

	/** Lay out a program for one mode so that the operand byte {@code v} is what gets used. */
	private int prepare(OpCodes op, String mode, int v) throws Exception {
		int[] operands;
		switch (mode) {
		case "IMM": operands = new int[] { v }; break;
		case "ZP": mem(0x30, v); operands = new int[] { 0x30 }; break;
		case "ZPX": x(0x10); mem(0x05, v); operands = new int[] { 0xf5 }; break;     // wraps to $05
		case "ABS": mem(0x2345, v); operands = new int[] { 0x45, 0x23 }; break;
		case "ABSX": x(0x0b); mem(0x2350, v); operands = new int[] { 0x45, 0x23 }; break;
		case "ABSY": y(0x0b); mem(0x2350, v); operands = new int[] { 0x45, 0x23 }; break;
		case "INX": x(0x04); mem(0x24, 0x74); mem(0x25, 0x20); mem(0x2074, v); operands = new int[] { 0x20 }; break;
		case "INY": y(0x10); mem(0x86, 0x28); mem(0x87, 0x40); mem(0x4038, v); operands = new int[] { 0x86 }; break;
		default: throw new IllegalArgumentException(mode);
		}
		c.bus.write(0x1000, op.code());
		for (int i = 0; i < operands.length; i++) {
			c.bus.write(0x1001 + i, (byte) operands[i]);
		}
		return 0x1001 + operands.length;
	}

	private static final String[] MODES = { "IMM", "ZP", "ZPX", "ABS", "ABSX", "ABSY", "INX", "INY" };

	@Test
	public void everyModeComputesTheRightResultAndAdvancesPc() throws Exception {
		for (int m = 0; m < MNEMONICS.length; m++) {
			for (String mode : MODES) {
				setup();
				OpCodes op = OpCodes.valueOf(name(MNEMONICS[m], mode));
				a(0xac);
				int expectedPc = prepare(op, mode, 0x0f);
				c.step();
				assertEquals(EXPECTED[m], c.a.get(), op + " result");
				assertEquals(expectedPc, c.pc, op + " pc");
			}
		}
	}

	@Test
	public void opcodesMatchTheRealInstructionSet() {
		int[][] expected = {
				// IMM   ZP    ZPX   ABS   ABSX  ABSY  INX   INY
				{ 0x29, 0x25, 0x35, 0x2d, 0x3d, 0x39, 0x21, 0x31 },     // AND ($2D, not the legacy $2F)
				{ 0x09, 0x05, 0x15, 0x0d, 0x1d, 0x19, 0x01, 0x11 },     // ORA
				{ 0x49, 0x45, 0x55, 0x4d, 0x5d, 0x59, 0x41, 0x51 } };   // EOR ($45, not the legacy $44)
		for (int m = 0; m < MNEMONICS.length; m++) {
			for (int i = 0; i < MODES.length; i++) {
				OpCodes op = OpCodes.valueOf(name(MNEMONICS[m], MODES[i]));
				assertEquals(expected[m][i], op.code() & 0xff, op.toString());
			}
		}
	}

	@Test
	public void flagsAndOperandsNotAffected() throws Exception {
		// AND -> zero
		a(0xf0);
		prepare(OpCodes.AND_IMM, "IMM", 0x0f);
		c.CFLAG.set(); c.OFLAG.set(); c.NFLAG.set(); c.ZFLAG.clear();
		c.step();
		assertEquals(0x00, c.a.get());
		assertEquals(true, c.ZFLAG.isSet());
		assertEquals(false, c.NFLAG.isSet());
		assertEquals(true, c.CFLAG.isSet(), "carry untouched");
		assertEquals(true, c.OFLAG.isSet(), "overflow untouched");

		// ORA -> negative
		c.pc = 0x1000;
		a(0x01);
		prepare(OpCodes.ORA_IMM, "IMM", 0x80);
		c.step();
		assertEquals(0x81, c.a.get());
		assertEquals(true, c.NFLAG.isSet());
		assertEquals(false, c.ZFLAG.isSet());

		// EOR -> positive non-zero clears stale flags
		c.pc = 0x1000;
		a(0xff);
		prepare(OpCodes.EOR_IMM, "IMM", 0x8f);
		c.NFLAG.set(); c.ZFLAG.set();
		c.step();
		assertEquals(0x70, c.a.get());
		assertEquals(false, c.NFLAG.isSet());
		assertEquals(false, c.ZFLAG.isSet());
	}

	@Test
	public void eorWithSelfClearsAccumulator() throws Exception {
		a(0x5a);
		prepare(OpCodes.EOR_IMM, "IMM", 0x5a);
		c.step();
		assertEquals(0, c.a.get());
		assertEquals(true, c.ZFLAG.isSet());
	}
}
