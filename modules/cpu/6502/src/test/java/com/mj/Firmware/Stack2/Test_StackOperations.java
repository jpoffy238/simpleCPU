package com.mj.Firmware.Stack2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.mj.Firmware.Framework.OpCodes;
import com.mj.cpu001.CPU;
import com.mj.util.CPU_CreateUtil;

/** The stack lives at $0100+SP; push = store then SP--, pull = SP++ then load. */
public class Test_StackOperations {
	private CPU c;

	@BeforeEach
	public void setup() throws Exception {
		c = CPU_CreateUtil.getCPU();
		c.step();            // consume power-on reset
		c.pc = 0x1000;
	}

	@Test
	public void resetStackPointerIsFD() {
		assertEquals(0xfd, c.sp);
	}

	@Test
	public void pushWritesAtSpThenDecrements() throws Exception {
		c.sp = 0xff;
		try { c.a.set(0x42); } catch (Exception e) { }
		c.bus.write(0x1000, OpCodes.PHA.code());
		c.step();
		assertEquals(0x42, c.bus.read(0x01ff) & 0xff);
		assertEquals(0xfe, c.sp);
	}

	@Test
	public void pullIncrementsThenReads() throws Exception {
		c.sp = 0xfe;
		c.bus.write(0x01ff, (byte) 0x99);
		c.bus.write(0x1000, OpCodes.PLA.code());
		c.step();
		assertEquals(0x99, c.a.get());
		assertEquals(0xff, c.sp);
		assertTrue(c.NFLAG.isSet());
		assertFalse(c.ZFLAG.isSet());
	}

	@Test
	public void plaClearsStaleFlags() throws Exception {
		c.ZFLAG.set();
		c.sp = 0xfe;
		c.bus.write(0x01ff, (byte) 0x10);
		c.bus.write(0x1000, OpCodes.PLA.code());
		c.step();
		assertFalse(c.ZFLAG.isSet());
		assertFalse(c.NFLAG.isSet());
	}

	@Test
	public void stackPointerWrapsWithinPageOne() throws Exception {
		c.sp = 0x00;
		try { c.a.set(0x77); } catch (Exception e) { }
		c.bus.write(0x1000, OpCodes.PHA.code());
		c.step();
		assertEquals(0x77, c.bus.read(0x0100) & 0xff);
		assertEquals(0xff, c.sp);
	}

	@Test
	public void txsAndTsx() throws Exception {
		try { c.x.set(0x80); } catch (Exception e) { }
		c.bus.write(0x1000, OpCodes.TXS.code());
		c.step();
		assertEquals(0x80, c.sp);
		c.x.reset();
		c.bus.write(0x1001, OpCodes.TSX.code());
		c.step();
		assertEquals(0x80, c.x.get());
		assertTrue(c.NFLAG.isSet());
	}

	@Test
	public void jsrPushesReturnAddressMinusOneAndRtsAddsOne() throws Exception {
		c.sp = 0xff;
		c.bus.write(0x1000, OpCodes.JSR.code());
		c.bus.write(0x1001, (byte) 0x00);
		c.bus.write(0x1002, (byte) 0x20);
		c.bus.write(0x2000, OpCodes.RTS.code());
		c.step();
		assertEquals(0x2000, c.pc);
		assertEquals(0xfd, c.sp);
		assertEquals(0x10, c.bus.read(0x01ff) & 0xff);   // high byte of $1002
		assertEquals(0x02, c.bus.read(0x01fe) & 0xff);   // low byte of $1002 (return - 1)
		c.step();
		assertEquals(0x1003, c.pc);
		assertEquals(0xff, c.sp);
	}
}
