package com.mj.Firmware.Load;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.mj.Firmware.Framework.OpCodes;
import com.mj.cpu001.CPU;
import com.mj.util.CPU_CreateUtil;

/** LDA in every addressing mode: value loaded, N/Z flags, and PC advance. */
public class Test_LDA_Modes {
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

	@Test
	public void immediate() throws Exception {
		program(OpCodes.LDA_IMM, 0x42);
		c.step();
		assertEquals(0x42, c.a.get());
		assertEquals(0x1002, c.pc);
	}

	@Test
	public void zeroPage() throws Exception {
		c.bus.write(0x0010, (byte) 0x37);
		program(OpCodes.LDA_ZP, 0x10);
		c.step();
		assertEquals(0x37, c.a.get());
		assertEquals(0x1002, c.pc);
	}

	@Test
	public void zeroPageXWrapsInPageZero() throws Exception {
		c.bus.write(0x0005, (byte) 0x55);
		x(0x10);
		program(OpCodes.LDA_ZX, 0xf5);        // $F5 + $10 = $105 -> $05
		c.step();
		assertEquals(0x55, c.a.get());
		assertEquals(0x1002, c.pc);
	}

	@Test
	public void absolute() throws Exception {
		c.bus.write(0x2345, (byte) 0x66);
		program(OpCodes.LDA_ABS, 0x45, 0x23);
		c.step();
		assertEquals(0x66, c.a.get());
		assertEquals(0x1003, c.pc);
	}

	@Test
	public void absoluteX() throws Exception {
		c.bus.write(0x2350, (byte) 0x77);
		x(0x0b);
		program(OpCodes.LDA_ABSX, 0x45, 0x23);
		c.step();
		assertEquals(0x77, c.a.get());
		assertEquals(0x1003, c.pc);
	}

	@Test
	public void absoluteYWrapsAt64K() throws Exception {
		c.bus.write(0x0010, (byte) 0x3c);
		y(0x20);
		program(OpCodes.LDA_ABSY, 0xf0, 0xff);   // $FFF0 + $20 -> $0010
		c.step();
		assertEquals(0x3c, c.a.get());
		assertEquals(0x1003, c.pc);
	}

	@Test
	public void indexedIndirectAdvancesPcByTwo() throws Exception {
		x(0x04);
		c.bus.write(0x0024, (byte) 0x74);        // pointer at $20+X = $24 -> $2074
		c.bus.write(0x0025, (byte) 0x20);
		c.bus.write(0x2074, (byte) 0x99);
		program(OpCodes.LDA_INDX, 0x20);
		c.step();
		assertEquals(0x99, c.a.get());
		assertEquals(0x1002, c.pc, "opcode + 1 operand byte");
	}

	@Test
	public void indirectIndexedAdvancesPcByTwo() throws Exception {
		y(0x10);
		c.bus.write(0x0086, (byte) 0x28);        // pointer $4028, + Y = $4038
		c.bus.write(0x0087, (byte) 0x40);
		c.bus.write(0x4038, (byte) 0x11);
		program(OpCodes.LDA_INDY, 0x86);
		c.step();
		assertEquals(0x11, c.a.get());
		assertEquals(0x1002, c.pc, "opcode + 1 operand byte");
	}

	@Test
	public void flagsFollowTheLoadedValue() throws Exception {
		program(OpCodes.LDA_IMM, 0x80);
		c.step();
		assertTrue(c.NFLAG.isSet());
		assertFalse(c.ZFLAG.isSet());

		c.pc = 0x1000;
		program(OpCodes.LDA_IMM, 0x00);
		c.step();
		assertTrue(c.ZFLAG.isSet());
		assertFalse(c.NFLAG.isSet());

		c.pc = 0x1000;
		program(OpCodes.LDA_IMM, 0x01);
		c.step();
		assertFalse(c.ZFLAG.isSet());
		assertFalse(c.NFLAG.isSet());
	}

	@Test
	public void loadDoesNotTouchCarryOrOverflow() throws Exception {
		c.CFLAG.set();
		c.OFLAG.set();
		program(OpCodes.LDA_IMM, 0x00);
		c.step();
		assertTrue(c.CFLAG.isSet());
		assertTrue(c.OFLAG.isSet());
	}
}
