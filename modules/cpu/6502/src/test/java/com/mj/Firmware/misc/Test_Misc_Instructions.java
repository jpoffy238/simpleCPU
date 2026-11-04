package com.mj.Firmware.misc;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.mj.Firmware.Framework.OpCodes;
import com.mj.cpu001.CPU;
import com.mj.util.CPU_CreateUtil;

/** BIT, register transfers, flag instructions, NOP and stack operations. */
public class Test_Misc_Instructions {
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
	private void run(OpCodes op, int... operands) throws Exception {
		c.pc = 0x1000;
		c.bus.write(0x1000, op.code());
		for (int i = 0; i < operands.length; i++) c.bus.write(0x1001 + i, (byte) operands[i]);
		c.step();
	}

	@Test public void opcodesMatchTheRealInstructionSet() {
		Object[][] expected = { { OpCodes.BIT_ZP, 0x24 }, { OpCodes.BIT_ABS, 0x2c }, { OpCodes.TAX, 0xaa }, { OpCodes.TAY, 0xa8 },
				{ OpCodes.TXA, 0x8a }, { OpCodes.TYA, 0x98 }, { OpCodes.TSX, 0xba }, { OpCodes.TXS, 0x9a }, { OpCodes.CLC, 0x18 },
				{ OpCodes.SEC, 0x38 }, { OpCodes.CLI, 0x58 }, { OpCodes.SEI, 0x78 }, { OpCodes.CLD, 0xd8 }, { OpCodes.SED, 0xf8 },
				{ OpCodes.CLV, 0xb8 }, { OpCodes.NOP, 0xea }, { OpCodes.PHA, 0x48 }, { OpCodes.PHP, 0x08 }, { OpCodes.PLA, 0x68 },
				{ OpCodes.PLP, 0x28 } };
		for (Object[] e : expected) assertEquals((Integer) e[1], ((OpCodes) e[0]).code() & 0xff, e[0].toString());
	}

	// ---- BIT ----------------------------------------------------------------
	@Test public void bitCopiesBits7And6AndTestsAnd() throws Exception {
		int[] values = { 0x00, 0x40, 0x80, 0xc0, 0x3f, 0xff };
		int[] accs = { 0x00, 0x0f, 0x40, 0x80, 0xff };
		for (int m : values) for (int acc : accs) for (int mode = 0; mode < 2; mode++) {
			c.bus.write(0x30, (byte) m);
			c.bus.write(0x2345, (byte) m);
			a(acc);
			c.CFLAG.set();                                       // must survive
			c.OFLAG.set(); c.NFLAG.set(); c.ZFLAG.set();         // stale
			if (mode == 0) run(OpCodes.BIT_ZP, 0x30); else run(OpCodes.BIT_ABS, 0x45, 0x23);
			String ctx = String.format("BIT %s A=%02x M=%02x", mode == 0 ? "zp" : "abs", acc, m);
			assertEquals((m & 0x80) != 0, c.NFLAG.isSet(), ctx + " N");
			assertEquals((m & 0x40) != 0, c.OFLAG.isSet(), ctx + " V");
			assertEquals((acc & m) == 0, c.ZFLAG.isSet(), ctx + " Z");
			assertEquals(true, c.CFLAG.isSet(), ctx + " C untouched");
			assertEquals(acc, c.a.get(), ctx + " A untouched");
			assertEquals(0x1001 + (mode == 0 ? 1 : 2), c.pc, ctx + " pc");
		}
	}

	// ---- transfers ----------------------------------------------------------
	@Test public void transfersCopyAndSetFlags() throws Exception {
		for (int v : new int[] { 0x00, 0x01, 0x7f, 0x80, 0xff }) {
			a(v); c.ZFLAG.set(); c.NFLAG.set();
			run(OpCodes.TAX);
			assertEquals(v, c.x.get());
			assertEquals(v == 0, c.ZFLAG.isSet());
			assertEquals((v & 0x80) != 0, c.NFLAG.isSet());
			a(0); run(OpCodes.TXA); assertEquals(v, c.a.get());
			a(0); y(0); run(OpCodes.TAY); assertEquals(0, c.y.get());
			a(v); run(OpCodes.TAY); assertEquals(v, c.y.get());
			a(0); run(OpCodes.TYA); assertEquals(v, c.a.get());
			assertEquals(0x1001, c.pc);
		}
	}

	@Test public void tsxSetsFlagsButTxsDoesNot() throws Exception {
		c.sp = 0x80;
		run(OpCodes.TSX);
		assertEquals(0x80, c.x.get());
		assertEquals(true, c.NFLAG.isSet());

		c.ZFLAG.clear(); c.NFLAG.clear();
		x(0x00);
		run(OpCodes.TXS);
		assertEquals(0x00, c.sp);
		assertEquals(false, c.ZFLAG.isSet(), "TXS must not set Z even though X is zero");
		assertEquals(false, c.NFLAG.isSet());
	}

	// ---- flags --------------------------------------------------------------
	@Test public void flagInstructionsAffectOnlyTheirFlag() throws Exception {
		c.CFLAG.clear(); c.IFLAG.clear(); c.DFLAG.clear(); c.OFLAG.set(); c.ZFLAG.set(); c.NFLAG.set();
		run(OpCodes.SEC); assertEquals(true, c.CFLAG.isSet());
		run(OpCodes.SEI); assertEquals(true, c.IFLAG.isSet());
		run(OpCodes.SED); assertEquals(true, c.DFLAG.isSet());
		run(OpCodes.CLV); assertEquals(false, c.OFLAG.isSet());
		run(OpCodes.CLC); assertEquals(false, c.CFLAG.isSet());
		run(OpCodes.CLI); assertEquals(false, c.IFLAG.isSet());
		run(OpCodes.CLD); assertEquals(false, c.DFLAG.isSet());
		assertEquals(true, c.ZFLAG.isSet(), "Z untouched");
		assertEquals(true, c.NFLAG.isSet(), "N untouched");
	}

	@Test public void nopChangesNothing() throws Exception {
		a(0x12); x(0x34); y(0x56); c.CFLAG.set();
		int sp = c.sp;
		run(OpCodes.NOP);
		assertEquals(0x12, c.a.get()); assertEquals(0x34, c.x.get()); assertEquals(0x56, c.y.get());
		assertEquals(true, c.CFLAG.isSet());
		assertEquals(sp, c.sp);
		assertEquals(0x1001, c.pc);
	}

	// ---- stack --------------------------------------------------------------
	@Test public void pushPullRoundTrip() throws Exception {
		a(0x9c);
		int sp = c.sp;
		run(OpCodes.PHA);
		assertEquals(sp - 1, c.sp);
		assertEquals(0x9c, c.bus.read(0x0100 | sp) & 0xff);
		a(0);
		run(OpCodes.PLA);
		assertEquals(0x9c, c.a.get());
		assertEquals(sp, c.sp);
		assertEquals(true, c.NFLAG.isSet());
	}

	@Test public void phpPushesBreakAndUnusedBitsAndPlpIgnoresThem() throws Exception {
		c.CFLAG.set(); c.ZFLAG.clear(); c.NFLAG.set(); c.OFLAG.clear(); c.DFLAG.clear(); c.IFLAG.clear();
		int sp = c.sp;
		run(OpCodes.PHP);
		assertEquals(0xb1, c.bus.read(0x0100 | sp) & 0xff, "N=1 V=0 unused=1 B=1 D=0 I=0 Z=0 C=1");
		c.CFLAG.clear(); c.NFLAG.clear();
		run(OpCodes.PLP);
		assertEquals(true, c.CFLAG.isSet());
		assertEquals(true, c.NFLAG.isSet());
		assertEquals(sp, c.sp);
	}
}
