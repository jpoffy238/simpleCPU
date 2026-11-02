package com.mj.Firmware.Load;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.mj.Firmware.Framework.OpCodes;
import com.mj.cpu001.CPU;
import com.mj.util.CPU_CreateUtil;

/** LDX and LDY in every addressing mode: value, correct index register, flags, PC advance. */
public class Test_LDX_LDY_Modes {
	private CPU c;

	@BeforeEach
	public void setup() throws Exception {
		c = CPU_CreateUtil.getCPU();
		c.step();               // consume power-on reset
		c.pc = 0x1000;
	}

	private void program(OpCodes op, int... operands) throws Exception {
		c.bus.write(0x1000, op.code());
		for (int i = 0; i < operands.length; i++) {
			c.bus.write(0x1001 + i, (byte) operands[i]);
		}
	}

	private void x(int v) { try { c.x.set(v); } catch (Exception e) { } }
	private void y(int v) { try { c.y.set(v); } catch (Exception e) { } }
	private void mem(int a, int v) throws Exception { c.bus.write(a, (byte) v); }

	// ---- LDX ----------------------------------------------------------------
	@Test public void ldxImmediate() throws Exception {
		program(OpCodes.LDX_IMM, 0x42);
		c.step();
		assertEquals(0x42, c.x.get());
		assertEquals(0x1002, c.pc);
	}

	@Test public void ldxZeroPage() throws Exception {
		mem(0x10, 0x37);
		program(OpCodes.LDX_ZP, 0x10);
		c.step();
		assertEquals(0x37, c.x.get());
		assertEquals(0x1002, c.pc);
	}

	@Test public void ldxZeroPageYWrapsInPageZero() throws Exception {
		mem(0x05, 0x55);
		y(0x10);
		program(OpCodes.LDX_ZPY, 0xf5);          // $F5 + $10 -> $05
		c.step();
		assertEquals(0x55, c.x.get());
		assertEquals(0x1002, c.pc);
	}

	@Test public void ldxAbsolute() throws Exception {
		mem(0x2345, 0x66);
		program(OpCodes.LDX_ABS, 0x45, 0x23);
		c.step();
		assertEquals(0x66, c.x.get());
		assertEquals(0x1003, c.pc);
	}

	@Test public void ldxAbsoluteYUsesYAsIndex() throws Exception {
		mem(0x2350, 0x77);
		y(0x0b);
		x(0x01);                                 // must not be used as the index
		program(OpCodes.LDX_ABSY, 0x45, 0x23);
		c.step();
		assertEquals(0x77, c.x.get());
		assertEquals(0x1003, c.pc);
	}

	// ---- LDY ----------------------------------------------------------------
	@Test public void ldyImmediate() throws Exception {
		program(OpCodes.LDY_IMM, 0x42);
		c.step();
		assertEquals(0x42, c.y.get());
		assertEquals(0x1002, c.pc);
	}

	@Test public void ldyZeroPage() throws Exception {
		mem(0x10, 0x37);
		program(OpCodes.LDY_ZP, 0x10);
		c.step();
		assertEquals(0x37, c.y.get());
		assertEquals(0x1002, c.pc);
	}

	@Test public void ldyZeroPageXWrapsInPageZero() throws Exception {
		mem(0x05, 0x55);
		x(0x10);
		program(OpCodes.LDY_ZPX, 0xf5);
		c.step();
		assertEquals(0x55, c.y.get());
		assertEquals(0x1002, c.pc);
	}

	@Test public void ldyAbsolute() throws Exception {
		mem(0x2345, 0x66);
		program(OpCodes.LDY_ABS, 0x45, 0x23);
		c.step();
		assertEquals(0x66, c.y.get());
		assertEquals(0x1003, c.pc);
	}

	/** The legacy class indexed with Y and loaded X; the correct behavior is index X, load Y. */
	@Test public void ldyAbsoluteXIndexesWithXAndLoadsY() throws Exception {
		mem(0x2350, 0x77);                       // $2345 + X($0b)
		mem(0x2352, 0x5a);                       // $2345 + Y($0d): the wrong target
		x(0x0b);
		y(0x0d);
		program(OpCodes.LDY_ABSX, 0x45, 0x23);
		c.step();
		assertEquals(0x77, c.y.get(), "Y receives the value");
		assertEquals(0x0b, c.x.get(), "X is left alone");
		assertEquals(0x1003, c.pc);
	}

	// ---- flags --------------------------------------------------------------
	@Test public void flagsFollowTheLoadedValue() throws Exception {
		for (OpCodes op : new OpCodes[] { OpCodes.LDX_IMM, OpCodes.LDY_IMM }) {
			for (int[] t : new int[][] { { 0x80, 1, 0 }, { 0x00, 0, 1 }, { 0x01, 0, 0 } }) {
				c.pc = 0x1000;
				c.NFLAG.set(); c.ZFLAG.set();                     // stale flags must be overwritten
				program(op, t[0]);
				c.step();
				assertEquals(t[1] == 1, c.NFLAG.isSet(), op + " N for " + t[0]);
				assertEquals(t[2] == 1, c.ZFLAG.isSet(), op + " Z for " + t[0]);
			}
		}
	}

	@Test public void loadsDoNotTouchCarryOrOverflow() throws Exception {
		c.CFLAG.set();
		c.OFLAG.set();
		program(OpCodes.LDX_IMM, 0x00);
		c.step();
		c.pc = 0x1000;
		program(OpCodes.LDY_IMM, 0x00);
		c.step();
		assertTrue(c.CFLAG.isSet());
		assertTrue(c.OFLAG.isSet());
	}

	@Test public void ldxLeavesYAndLdyLeavesX() throws Exception {
		x(0x11); y(0x22);
		program(OpCodes.LDX_IMM, 0x33);
		c.step();
		assertEquals(0x22, c.y.get());
		c.pc = 0x1000;
		program(OpCodes.LDY_IMM, 0x44);
		c.step();
		assertEquals(0x33, c.x.get());
		assertFalse(c.x.get() == c.y.get());
	}
}
