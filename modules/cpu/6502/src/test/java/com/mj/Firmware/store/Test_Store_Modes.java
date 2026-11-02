package com.mj.Firmware.store;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.mj.Firmware.Framework.OpCodes;
import com.mj.cpu001.CPU;
import com.mj.util.CPU_CreateUtil;

/** STA / STX / STY in every addressing mode: stored value, target address, flags untouched, PC advance. */
public class Test_Store_Modes {
	private CPU c;

	@BeforeEach
	public void setup() throws Exception {
		c = CPU_CreateUtil.getCPU();
		c.step();               // consume power-on reset
		c.pc = 0x1000;
		a(0xa5); x(0x11); y(0x22);
	}

	private void program(OpCodes op, int... operands) throws Exception {
		c.bus.write(0x1000, op.code());
		for (int i = 0; i < operands.length; i++) {
			c.bus.write(0x1001 + i, (byte) operands[i]);
		}
	}

	private void a(int v) { try { c.a.set(v); } catch (Exception e) { } }
	private void x(int v) { try { c.x.set(v); } catch (Exception e) { } }
	private void y(int v) { try { c.y.set(v); } catch (Exception e) { } }
	private void mem(int a, int v) throws Exception { c.bus.write(a, (byte) v); }
	private int at(int a) throws Exception { return c.bus.read(a) & 0xff; }

	// ---- opcodes ------------------------------------------------------------
	@Test public void opcodesMatchTheRealInstructionSet() {
		assertEquals(0x85, OpCodes.STA_ZP.code() & 0xff);
		assertEquals(0x95, OpCodes.STA_ZPX.code() & 0xff);
		assertEquals(0x8d, OpCodes.STA_ABS.code() & 0xff);
		assertEquals(0x9d, OpCodes.STA_ABX.code() & 0xff);
		assertEquals(0x99, OpCodes.STA_ABY.code() & 0xff);
		assertEquals(0x81, OpCodes.STA_INX.code() & 0xff);
		assertEquals(0x91, OpCodes.STA_INY.code() & 0xff);
		assertEquals(0x86, OpCodes.STX_ZP.code() & 0xff);    // legacy had $8E here
		assertEquals(0x96, OpCodes.STX_ZPY.code() & 0xff);
		assertEquals(0x8e, OpCodes.STX_ABS.code() & 0xff);   // legacy had $86 here
		assertEquals(0x84, OpCodes.STY_ZP.code() & 0xff);
		assertEquals(0x94, OpCodes.STY_ZPX.code() & 0xff);
		assertEquals(0x8c, OpCodes.STY_ABS.code() & 0xff);
	}

	// ---- STA ----------------------------------------------------------------
	@Test public void staZeroPage() throws Exception {
		program(OpCodes.STA_ZP, 0x10);
		c.step();
		assertEquals(0xa5, at(0x10));
		assertEquals(0x1002, c.pc);
	}

	@Test public void staZeroPageXWraps() throws Exception {
		x(0x10);
		program(OpCodes.STA_ZPX, 0xf5);          // -> $05
		c.step();
		assertEquals(0xa5, at(0x05));
		assertEquals(0x1002, c.pc);
	}

	@Test public void staAbsolute() throws Exception {
		program(OpCodes.STA_ABS, 0x45, 0x23);
		c.step();
		assertEquals(0xa5, at(0x2345));
		assertEquals(0x1003, c.pc);
	}

	@Test public void staAbsoluteXAndY() throws Exception {
		x(0x0b);
		program(OpCodes.STA_ABX, 0x45, 0x23);
		c.step();
		assertEquals(0xa5, at(0x2350));
		assertEquals(0x1003, c.pc);

		c.pc = 0x1000;
		y(0x21);
		a(0x5a);
		program(OpCodes.STA_ABY, 0x45, 0x23);
		c.step();
		assertEquals(0x5a, at(0x2366));
		assertEquals(0x1003, c.pc);
	}

	@Test public void staIndexedIndirect() throws Exception {
		x(0x04);
		mem(0x24, 0x74); mem(0x25, 0x20);
		program(OpCodes.STA_INX, 0x20);
		c.step();
		assertEquals(0xa5, at(0x2074));
		assertEquals(0x1002, c.pc);
	}

	@Test public void staIndirectIndexed() throws Exception {
		y(0x10);
		mem(0x86, 0x28); mem(0x87, 0x40);
		program(OpCodes.STA_INY, 0x86);
		c.step();
		assertEquals(0xa5, at(0x4038));
		assertEquals(0x1002, c.pc);
	}

	// ---- STX ----------------------------------------------------------------
	@Test public void stxZeroPageStoresXNotTheAddress() throws Exception {
		x(0x7e);
		program(OpCodes.STX_ZP, 0x30);
		c.step();
		assertEquals(0x7e, at(0x30));
		assertEquals(0x1002, c.pc);
	}

	@Test public void stxZeroPageYWraps() throws Exception {
		x(0x7e); y(0x10);
		program(OpCodes.STX_ZPY, 0xf5);
		c.step();
		assertEquals(0x7e, at(0x05));
		assertEquals(0x1002, c.pc);
	}

	@Test public void stxAbsolute() throws Exception {
		x(0x7e);
		program(OpCodes.STX_ABS, 0x45, 0x23);
		c.step();
		assertEquals(0x7e, at(0x2345));
		assertEquals(0x1003, c.pc);
	}

	// ---- STY ----------------------------------------------------------------
	@Test public void styZeroPage() throws Exception {
		y(0x66);
		program(OpCodes.STY_ZP, 0x30);
		c.step();
		assertEquals(0x66, at(0x30));
		assertEquals(0x1002, c.pc);
	}

	@Test public void styZeroPageXWraps() throws Exception {
		y(0x66); x(0x10);
		program(OpCodes.STY_ZPX, 0xf5);
		c.step();
		assertEquals(0x66, at(0x05));
		assertEquals(0x1002, c.pc);
	}

	@Test public void styAbsoluteAdvancesPcByThree() throws Exception {
		y(0x66);
		program(OpCodes.STY_ABS, 0x45, 0x23);
		c.step();
		assertEquals(0x66, at(0x2345));
		assertEquals(0x1003, c.pc, "legacy advanced by only 1");
	}

	// ---- no flag effects ------------------------------------------------------
	@Test public void storesDoNotAffectAnyFlag() throws Exception {
		a(0x00);                                          // a zero/negative value must not set Z/N
		c.CFLAG.set(); c.OFLAG.set(); c.ZFLAG.clear(); c.NFLAG.clear();
		program(OpCodes.STA_ZP, 0x10);
		c.step();
		assertEquals(false, c.ZFLAG.isSet());
		assertEquals(false, c.NFLAG.isSet());
		assertEquals(true, c.CFLAG.isSet());
		assertEquals(true, c.OFLAG.isSet());
	}
}
