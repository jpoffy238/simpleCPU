package com.mj.Firmware.Shift;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.mj.Firmware.Framework.OpCodes;
import com.mj.cpu001.CPU;
import com.mj.util.CPU_CreateUtil;

/** Flag and value behavior of ASL / LSR / ROL / ROR in every addressing mode. */
public class Test_ShiftRotate {
	private CPU c;

	@BeforeEach
	public void setup() throws Exception {
		c = CPU_CreateUtil.getCPU();
		c.step();            // consume power-on reset
		c.pc = 0x1000;
	}

	private void flags(boolean carry) {
		if (carry) c.CFLAG.set(); else c.CFLAG.clear();
		c.ZFLAG.clear(); c.NFLAG.clear();
	}

	private void zpOp(OpCodes op, int value, boolean carryIn) throws Exception {
		c.bus.write(0x1000, op.code());
		c.bus.write(0x1001, (byte) 0x20);
		c.bus.write(0x0020, (byte) value);
		flags(carryIn);
		c.step();
	}

	private int mem() throws Exception { return c.bus.read(0x0020) & 0xff; }
	private int f(boolean b) { return b ? 1 : 0; }

	private void check(int expValue, int c_, int z, int n) throws Exception {
		assertEquals(expValue, mem());
		assertEquals(c_, f(c.CFLAG.isSet()), "C");
		assertEquals(z, f(c.ZFLAG.isSet()), "Z");
		assertEquals(n, f(c.NFLAG.isSet()), "N");
		assertEquals(0x1002, c.pc);
	}

	@Test public void aslCases() throws Exception {
		zpOp(OpCodes.ASL_ZP, 0x40, true);  check(0x80, 0, 0, 1);   // carry cleared
		setup(); zpOp(OpCodes.ASL_ZP, 0x80, false); check(0x00, 1, 1, 0); // carry out and zero
		setup(); zpOp(OpCodes.ASL_ZP, 0xC1, false); check(0x82, 1, 0, 1); // carry out AND negative
	}

	@Test public void lsrCases() throws Exception {
		zpOp(OpCodes.LSR_ZP, 0x01, false); check(0x00, 1, 1, 0);
		setup(); zpOp(OpCodes.LSR_ZP, 0xFF, true);  check(0x7F, 1, 0, 0);   // N always clear
		setup(); zpOp(OpCodes.LSR_ZP, 0x04, true);  check(0x02, 0, 0, 0);
	}

	@Test public void rolCases() throws Exception {
		zpOp(OpCodes.ROL_ZP, 0x80, false); check(0x00, 1, 1, 0);
		setup(); zpOp(OpCodes.ROL_ZP, 0x80, true);  check(0x01, 1, 0, 0);   // carry out and in
		setup(); zpOp(OpCodes.ROL_ZP, 0x40, true);  check(0x81, 0, 0, 1);
		setup(); zpOp(OpCodes.ROL_ZP, 0xC0, false); check(0x80, 1, 0, 1);   // carry out AND negative
	}

	@Test public void rorCases() throws Exception {
		zpOp(OpCodes.ROR_ZP, 0x01, false); check(0x00, 1, 1, 0);   // carry from bit 0
		setup(); zpOp(OpCodes.ROR_ZP, 0x01, true);  check(0x80, 1, 0, 1);
		setup(); zpOp(OpCodes.ROR_ZP, 0xFE, false); check(0x7F, 0, 0, 0);   // no sign extension
		setup(); zpOp(OpCodes.ROR_ZP, 0x02, true);  check(0x81, 0, 0, 1);
	}

	@Test public void lsrAccumulator() throws Exception {
		c.a.reset();
		try { c.a.set(0x83); } catch (Exception e) { }
		c.bus.write(0x1000, OpCodes.LSR_ACC.code());
		flags(false);
		c.step();
		assertEquals(0x41, c.a.get());
		assertEquals(1, f(c.CFLAG.isSet()));
		assertEquals(0, f(c.NFLAG.isSet()));
		assertEquals(0x1001, c.pc);
	}

	@Test public void rolRorAccumulator() throws Exception {
		try { c.a.set(0x81); } catch (Exception e) { }
		c.bus.write(0x1000, OpCodes.ROL_ACC.code());
		c.bus.write(0x1001, OpCodes.ROR_ACC.code());
		flags(false);
		c.step();                                  // ROL: 0x81 -> 0x02, C=1
		assertEquals(0x02, c.a.get());
		assertEquals(1, f(c.CFLAG.isSet()));
		c.step();                                  // ROR: 0x02 with C=1 -> 0x81, C=0
		assertEquals(0x81, c.a.get());
		assertEquals(0, f(c.CFLAG.isSet()));
		assertEquals(1, f(c.NFLAG.isSet()));
	}

	@Test public void lsrOtherAddressingModes() throws Exception {
		// ZP,X
		c.x.reset(); try { c.x.set(2); } catch (Exception e) { }
		c.bus.write(0x1000, OpCodes.LSR_ZPX.code()); c.bus.write(0x1001, (byte) 0x1e);
		c.bus.write(0x0020, (byte) 0x03);
		c.step();
		assertEquals(0x01, mem()); assertEquals(1, f(c.CFLAG.isSet()));
		// ABS
		c.pc = 0x1000;
		c.bus.write(0x1000, OpCodes.LSR_ABS.code()); c.bus.write(0x1001, (byte) 0x00); c.bus.write(0x1002, (byte) 0x20);
		c.bus.write(0x2000, (byte) 0x08);
		c.step();
		assertEquals(0x04, c.bus.read(0x2000) & 0xff); assertEquals(0x1003, c.pc);
		// ABS,X
		c.pc = 0x1000;
		c.bus.write(0x1000, OpCodes.LSR_ABX.code()); c.bus.write(0x1001, (byte) 0xfe); c.bus.write(0x1002, (byte) 0x1f);
		c.bus.write(0x2000, (byte) 0x10);
		c.step();
		assertEquals(0x08, c.bus.read(0x2000) & 0xff); assertEquals(0x1003, c.pc);
	}
}
