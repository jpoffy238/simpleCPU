package com.mj.Firmware.Status;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.mj.cpu001.CPU;
import com.mj.util.CPU_CreateUtil;

/** Processor status byte uses the real layout N V 1 B D I Z C. */
public class Test_StatusRegister {
	private static final int PHP = 0x08, PLP = 0x28, BRK = 0x00, RTI = 0x40;
	private CPU c;

	@BeforeEach
	public void setup() throws Exception {
		c = CPU_CreateUtil.getCPU();
		c.step();                 // consume pending power-on reset
		c.pc = 0x1000;
		c.sp = 0xff;
		c.CFLAG.clear(); c.ZFLAG.clear(); c.IFLAG.clear();
		c.DFLAG.clear(); c.OFLAG.clear(); c.NFLAG.clear();
	}

	private int pushedStatus() throws Exception {
		return c.bus.read(0x0100 | ((c.sp + 1) & 0xff)) & 0xff;   // last pushed byte
	}

	@Test
	public void phpAllClearPushesBreakAndUnusedBits() throws Exception {
		c.bus.write(0x1000, (byte) PHP);
		c.step();
		assertEquals(0x30, pushedStatus());     // 0011 0000
	}

	@Test
	public void phpEachFlagInItsOwnBit() throws Exception {
				int[] expected = { 0x01, 0x02, 0x04, 0x08, 0x40, 0x80 };
		for (int i = 0; i < expected.length; i++) {
			setup();
			switch (i) {
			case 0: c.CFLAG.set(); break;
			case 1: c.ZFLAG.set(); break;
			case 2: c.IFLAG.set(); break;
			case 3: c.DFLAG.set(); break;
			case 4: c.OFLAG.set(); break;
			default: c.NFLAG.set(); break;
			}
			c.bus.write(0x1000, (byte) PHP);
			c.step();
			assertEquals(0x30 | expected[i], pushedStatus(), "flag index " + i);
		}
	}

	@Test
	public void plpLoadsFlagsAndIgnoresBreakBit() throws Exception {
		c.sp = 0xfd;
		c.bus.write(0x01fe, (byte) 0xC3);          // N V - - - - Z C
		c.bus.write(0x1000, (byte) PLP);
		c.step();
		assertTrue(c.NFLAG.isSet());
		assertTrue(c.OFLAG.isSet());
		assertTrue(c.ZFLAG.isSet());
		assertTrue(c.CFLAG.isSet());
		assertFalse(c.IFLAG.isSet());
		assertFalse(c.DFLAG.isSet());
	}

	@Test
	public void phpPlpRoundTrip() throws Exception {
		c.CFLAG.set(); c.DFLAG.set(); c.NFLAG.set();
		c.bus.write(0x1000, (byte) PHP);
		c.bus.write(0x1001, (byte) PLP);
		c.step();
		c.CFLAG.clear(); c.DFLAG.clear(); c.NFLAG.clear();
		c.step();
		assertTrue(c.CFLAG.isSet());
		assertTrue(c.DFLAG.isSet());
		assertTrue(c.NFLAG.isSet());
		assertFalse(c.ZFLAG.isSet());
		assertFalse(c.OFLAG.isSet());
		assertFalse(c.IFLAG.isSet());
	}

	@Test
	public void brkPushesOldStatusThenSetsInterruptDisable() throws Exception {
		c.CFLAG.set();
		c.bus.write(0x1000, (byte) BRK);
		c.step();
		assertTrue(c.IFLAG.isSet());
		// pushed: B and unused bits set, I still clear (old status), C set
		assertEquals(0x30 | 0x01, pushedStatus());
	}

	@Test
	public void rtiRestoresPushedInterruptFlag() throws Exception {
		// Case 1: interrupt taken with I clear -> I clear after RTI
		c.bus.write(0x1000, (byte) BRK);
		c.step();
		c.pc = 0x1100;
		c.bus.write(0x1100, (byte) RTI);
		c.step();
		assertFalse(c.IFLAG.isSet());
		assertEquals(0x1002, c.pc);   // BRK returns to BRK address + 2

		// Case 2: I already set before the interrupt -> I stays set after RTI
		setup();
		c.IFLAG.set();
		c.bus.write(0x1000, (byte) BRK);
		c.step();
		c.pc = 0x1100;
		c.bus.write(0x1100, (byte) RTI);
		c.step();
		assertTrue(c.IFLAG.isSet());
	}
}
