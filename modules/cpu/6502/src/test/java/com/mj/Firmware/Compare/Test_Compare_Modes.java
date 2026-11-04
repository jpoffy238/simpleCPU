package com.mj.Firmware.Compare;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.mj.Firmware.Framework.OpCodes;
import com.mj.cpu001.CPU;
import com.mj.util.CPU_CreateUtil;

/**
 * CMP / CPX / CPY in every mode: an edge-value matrix checked against an independent model.
 * C = reg >= operand (unsigned, so also set when equal), Z = equal, N = bit 7 of (reg - operand).
 */
public class Test_Compare_Modes {
	private CPU c;

	@BeforeEach
	public void setup() throws Exception {
		c = CPU_CreateUtil.getCPU();
		c.step();               // consume power-on reset
	}

	private static final int[] VALUES = { 0x00, 0x01, 0x40, 0x7f, 0x80, 0x81, 0xfe, 0xff };
	private static final String[] CMP_MODES = { "IMM", "ZP", "ZPX", "ABS", "ABX", "ABY", "INX", "INY" };
	private static final int[] CMP_OPCODES = { 0xc9, 0xc5, 0xd5, 0xcd, 0xdd, 0xd9, 0xc1, 0xd1 };
	private static final int[] CMP_OPERAND_BYTES = { 1, 1, 1, 2, 2, 2, 1, 1 };
	private static final String[] SMALL_MODES = { "IMM", "ZP", "ABS" };
	private static final int[] CPX_OPCODES = { 0xe0, 0xe4, 0xec };
	private static final int[] CPY_OPCODES = { 0xc0, 0xc4, 0xcc };
	private static final int[] SMALL_OPERAND_BYTES = { 1, 1, 2 };

	private void a(int v) { try { c.a.set(v); } catch (Exception e) { } }
	private void x(int v) { try { c.x.set(v); } catch (Exception e) { } }
	private void y(int v) { try { c.y.set(v); } catch (Exception e) { } }

	/** Lay out one mode so that {@code operand} is what the instruction compares against. */
	private int[] arrange(String mode, int operand) throws Exception {
		switch (mode) {
		case "IMM": return new int[] { operand };
		case "ZP": c.bus.write(0x30, (byte) operand); return new int[] { 0x30 };
		case "ZPX": x(0x10); c.bus.write(0x05, (byte) operand); return new int[] { 0xf5 };       // wraps to $05
		case "ABS": c.bus.write(0x2345, (byte) operand); return new int[] { 0x45, 0x23 };
		case "ABX": x(0x0b); c.bus.write(0x2350, (byte) operand); return new int[] { 0x45, 0x23 };
		case "ABY": y(0x0b); c.bus.write(0x2350, (byte) operand); return new int[] { 0x45, 0x23 };
		case "INX": x(0x04); c.bus.write(0x24, (byte) 0x74); c.bus.write(0x25, (byte) 0x20);
			c.bus.write(0x2074, (byte) operand); return new int[] { 0x20 };
		default: y(0x10); c.bus.write(0x86, (byte) 0x28); c.bus.write(0x87, (byte) 0x40);
			c.bus.write(0x4038, (byte) operand); return new int[] { 0x86 };
		}
	}

	/** Runs one compare and checks flags, untouched state and PC against the model. */
	private void runCase(OpCodes op, String mode, int reg, int operand, char regName, int operandBytes) throws Exception {
		c.pc = 0x1000;
		int[] bytes = arrange(mode, operand);
		// a compare register that doubles as an index must be set after arranging
		switch (regName) {
		case 'A': a(reg); break;
		case 'X': x(reg); break;
		default: y(reg); break;
		}
		c.bus.write(0x1000, op.code());
		for (int i = 0; i < bytes.length; i++) c.bus.write(0x1001 + i, (byte) bytes[i]);
		// stale and must-survive flags
		c.CFLAG.set(); c.ZFLAG.set(); c.NFLAG.set();
		c.OFLAG.clear(); c.DFLAG.set();
		if (reg >= operand) c.CFLAG.clear(); else c.CFLAG.set();     // start from the opposite state

		c.step();

		String ctx = String.format("%s reg=%02x operand=%02x", op, reg, operand);
		assertEquals(reg >= operand, c.CFLAG.isSet(), ctx + " carry");
		assertEquals(reg == operand, c.ZFLAG.isSet(), ctx + " zero");
		assertEquals(((reg - operand) & 0x80) != 0, c.NFLAG.isSet(), ctx + " negative");
		assertEquals(false, c.OFLAG.isSet(), ctx + " overflow untouched");
		assertEquals(true, c.DFLAG.isSet(), ctx + " decimal untouched");
		int actualReg = regName == 'A' ? c.a.get() : regName == 'X' ? c.x.get() : c.y.get();
		assertEquals(reg, actualReg, ctx + " register unchanged");
		assertEquals(0x1001 + operandBytes, c.pc, ctx + " pc");
	}

	@Test
	public void cmpEdgeMatrixInEveryMode() throws Exception {
		for (int m = 0; m < CMP_MODES.length; m++) {
			OpCodes op = OpCodes.valueOf("CMP_" + CMP_MODES[m]);
			assertEquals(CMP_OPCODES[m], op.code() & 0xff, op + " opcode");
			for (int reg : VALUES) for (int operand : VALUES) {
				// X/Y are index registers in some modes: the compare register is A, so no clash
				runCase(op, CMP_MODES[m], reg, operand, 'A', CMP_OPERAND_BYTES[m]);
			}
		}
	}

	@Test
	public void cpxAndCpyEdgeMatrix() throws Exception {
		for (int m = 0; m < SMALL_MODES.length; m++) {
			OpCodes cpx = OpCodes.valueOf("CPX_" + SMALL_MODES[m]);
			OpCodes cpy = OpCodes.valueOf("CPY_" + SMALL_MODES[m]);
			assertEquals(CPX_OPCODES[m], cpx.code() & 0xff, cpx + " opcode");
			assertEquals(CPY_OPCODES[m], cpy.code() & 0xff, cpy + " opcode");
			for (int reg : VALUES) for (int operand : VALUES) {
				runCase(cpx, SMALL_MODES[m], reg, operand, 'X', SMALL_OPERAND_BYTES[m]);
				runCase(cpy, SMALL_MODES[m], reg, operand, 'Y', SMALL_OPERAND_BYTES[m]);
			}
		}
	}

	/** The case ADC_Test.asm depends on: CPY #1 with Y = 1 must SET carry. */
	@Test
	public void equalOperandsSetCarry() throws Exception {
		y(1);
		c.pc = 0x1000;
		c.bus.write(0x1000, OpCodes.CPY_IMM.code());
		c.bus.write(0x1001, (byte) 1);
		c.CFLAG.clear();
		c.step();
		assertEquals(true, c.CFLAG.isSet());
		assertEquals(true, c.ZFLAG.isSet());
		assertEquals(false, c.NFLAG.isSet());
	}

	/** The legacy classes treated the operand as a signed byte: $10 vs $80 reported "greater". */
	@Test
	public void operandsAboveSevenFAreUnsigned() throws Exception {
		a(0x10);
		c.pc = 0x1000;
		c.bus.write(0x1000, OpCodes.CMP_IMM.code());
		c.bus.write(0x1001, (byte) 0x80);
		c.step();
		assertEquals(false, c.CFLAG.isSet(), "$10 < $80 unsigned, so carry is clear");
		assertEquals(false, c.ZFLAG.isSet());
		assertEquals(true, c.NFLAG.isSet(), "$10 - $80 = $90, bit 7 set");
	}
}
