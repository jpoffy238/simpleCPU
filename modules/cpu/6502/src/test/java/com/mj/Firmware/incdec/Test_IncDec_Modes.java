package com.mj.Firmware.incdec;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.mj.Firmware.Framework.OpCodes;
import com.mj.cpu001.CPU;
import com.mj.util.CPU_CreateUtil;

/** INC / DEC on memory in every mode, and INX / INY / DEX / DEY on the registers. */
public class Test_IncDec_Modes {
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
	private int at(int a) throws Exception { return c.bus.read(a) & 0xff; }

	@Test public void opcodesMatchTheRealInstructionSet() {
		assertEquals(0xe6, OpCodes.INC_ZP.code() & 0xff);
		assertEquals(0xf6, OpCodes.INC_ZPX.code() & 0xff);
		assertEquals(0xee, OpCodes.INC_ABS.code() & 0xff);
		assertEquals(0xfe, OpCodes.INC_ABX.code() & 0xff);
		assertEquals(0xc6, OpCodes.DEC_ZP.code() & 0xff);
		assertEquals(0xd6, OpCodes.DEC_ZPX.code() & 0xff);
		assertEquals(0xce, OpCodes.DEC_ABS.code() & 0xff);
		assertEquals(0xde, OpCodes.DEC_ABX.code() & 0xff);
		assertEquals(0xe8, OpCodes.INX.code() & 0xff);
		assertEquals(0xc8, OpCodes.INY.code() & 0xff);
		assertEquals(0xca, OpCodes.DEX.code() & 0xff);
		assertEquals(0x88, OpCodes.DEY.code() & 0xff);
	}

	// ---- memory -------------------------------------------------------------
	@Test public void incZeroPage() throws Exception {
		mem(0x10, 0x41);
		program(OpCodes.INC_ZP, 0x10);
		c.step();
		assertEquals(0x42, at(0x10));
		assertEquals(0x1002, c.pc);
	}

	@Test public void incZeroPageXWraps() throws Exception {
		x(0x10); mem(0x05, 0x01);
		program(OpCodes.INC_ZPX, 0xf5);
		c.step();
		assertEquals(0x02, at(0x05));
		assertEquals(0x1002, c.pc);
	}

	@Test public void incAbsoluteAndAbsoluteX() throws Exception {
		mem(0x2345, 0x09);
		program(OpCodes.INC_ABS, 0x45, 0x23);
		c.step();
		assertEquals(0x0a, at(0x2345));
		assertEquals(0x1003, c.pc);

		c.pc = 0x1000;
		x(0x0b); mem(0x2350, 0x20);
		program(OpCodes.INC_ABX, 0x45, 0x23);
		c.step();
		assertEquals(0x21, at(0x2350));
		assertEquals(0x1003, c.pc);
	}

	@Test public void decZeroPage() throws Exception {
		mem(0x10, 0x42);
		program(OpCodes.DEC_ZP, 0x10);
		c.step();
		assertEquals(0x41, at(0x10));
		assertEquals(0x1002, c.pc);
	}

	/** The legacy DEC zp,X incremented instead of decrementing. */
	@Test public void decZeroPageXDecrementsAndWraps() throws Exception {
		x(0x10); mem(0x05, 0x10);
		program(OpCodes.DEC_ZPX, 0xf5);
		c.step();
		assertEquals(0x0f, at(0x05));
		assertEquals(0x1002, c.pc);
	}

	@Test public void decAbsoluteAndAbsoluteX() throws Exception {
		mem(0x2345, 0x0a);
		program(OpCodes.DEC_ABS, 0x45, 0x23);
		c.step();
		assertEquals(0x09, at(0x2345));
		assertEquals(0x1003, c.pc);

		c.pc = 0x1000;
		x(0x0b); mem(0x2350, 0x21);
		program(OpCodes.DEC_ABX, 0x45, 0x23);
		c.step();
		assertEquals(0x20, at(0x2350));
		assertEquals(0x1003, c.pc);
	}

	@Test public void memoryWrapsAndSetsFlags() throws Exception {
		mem(0x10, 0xff);
		program(OpCodes.INC_ZP, 0x10);
		c.step();
		assertEquals(0x00, at(0x10));
		assertEquals(true, c.ZFLAG.isSet());
		assertEquals(false, c.NFLAG.isSet());

		c.pc = 0x1000;
		program(OpCodes.DEC_ZP, 0x10);
		c.step();                                   // 0x00 -> 0xFF
		assertEquals(0xff, at(0x10));
		assertEquals(false, c.ZFLAG.isSet());
		assertEquals(true, c.NFLAG.isSet());
	}

	@Test public void carryAndOverflowAreNotAffected() throws Exception {
		c.CFLAG.set(); c.OFLAG.set();
		mem(0x10, 0xff);
		program(OpCodes.INC_ZP, 0x10);
		c.step();
		assertEquals(true, c.CFLAG.isSet());
		assertEquals(true, c.OFLAG.isSet());
	}

	// ---- registers ----------------------------------------------------------
	@Test public void inxIny() throws Exception {
		x(0x7f); y(0xff);
		program(OpCodes.INX);
		c.step();
		assertEquals(0x80, c.x.get());
		assertEquals(true, c.NFLAG.isSet());
		assertEquals(0x1001, c.pc, "implied: no operand");

		c.pc = 0x1000;
		program(OpCodes.INY);
		c.step();
		assertEquals(0x00, c.y.get());
		assertEquals(true, c.ZFLAG.isSet());
		assertEquals(false, c.NFLAG.isSet());
		assertEquals(0x80, c.x.get(), "INY leaves X alone");
	}

	@Test public void dexDey() throws Exception {
		x(0x01); y(0x00);
		program(OpCodes.DEX);
		c.step();
		assertEquals(0x00, c.x.get());
		assertEquals(true, c.ZFLAG.isSet());
		assertEquals(0x1001, c.pc);

		c.pc = 0x1000;
		program(OpCodes.DEY);
		c.step();
		assertEquals(0xff, c.y.get());
		assertEquals(true, c.NFLAG.isSet());
		assertEquals(false, c.ZFLAG.isSet());
		assertEquals(0x00, c.x.get(), "DEY leaves X alone");
	}
}
