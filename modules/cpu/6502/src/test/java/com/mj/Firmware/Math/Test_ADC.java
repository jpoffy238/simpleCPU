package com.mj.Firmware.Math;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.mj.cpu001.CPU;
import com.mj.exceptions.DeviceUnavailable;
import com.mj.exceptions.ROException;
import com.mj.exceptions.illegalAddressException;
import com.mj.util.CPU_CreateUtil;

public class Test_ADC {
	private CPU c;

	private static final Logger logger = LogManager.getLogger(Test_ADC.class);

	@BeforeEach
	public void setup() {
		c = CPU_CreateUtil.getCPU();
		logger.debug("In Setup -- Current cpu state: " + CPU.currentThread().getState());
	}

	/**
	 * Bruce Clark's exhaustive V-flag test: runs ADC and SBC for every combination of
	 * two operands and both carry values (131,072 cases) and checks that the V flag
	 * agrees with the unsigned result. The program stores 1 in ERROR when it starts and
	 * 0 only after every case passes, so a sentinel is pre-loaded to make sure the
	 * program really ran to completion.
	 *
	 * SLOW (minutes): the arithmetic itself is already verified exhaustively and quickly by
	 * Test_AddSub_Exhaustive, so this is a second opinion run as real machine code. It is opt-in.
	 */
	@org.junit.jupiter.api.Tag("slow")
	@org.junit.jupiter.api.condition.EnabledIfSystemProperty(named = "slowTests", matches = "true",
			disabledReason = "Runs Clark's 131,072-case program through the full CPU loop (minutes). Opt in with: mvn test -DslowTests=true")
	@Test
	public void Test_ADC_Advanced() throws Exception {
		final int ERROR = 0x2000;
		CPU_CreateUtil.load(c.bus, "ADC_Test.hex", 0);
		c.bus.write(ERROR, (byte) 0xee);               // sentinel: neither "pass" (0) nor "fail" (1)
		c.pc = 0x1000;
		c.run();
		int results = c.bus.read(ERROR) & 0xff;
		logger.debug(String.format("Computed Value = %02x", results));
		assertEquals(0, results, "ERROR: 0xEE = program never ran, 1 = a case failed, 0 = all passed");
	}

	@Test
	public void Test_ADC_BCD01() {
		try {
			CPU_CreateUtil.load(c.bus, "ADC_DEC_mode.hex", 0);

		} catch (IOException | illegalAddressException | ROException | DeviceUnavailable e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			fail("Unable to load test program");

		}
		c.pc = 0x1000;
		c.run();

		int results = c.a.get();

		logger.debug(String.format("Computed Value = %02x", results));
		assert (results == 0x10);

	}
	@Test
	public void Test_ADC_BCD02() {
		try {
			CPU_CreateUtil.load(c.bus, "ADC_DEC_mode2.hex", 0);

		} catch (IOException | illegalAddressException | ROException | DeviceUnavailable e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			fail("Unable to load test program");

		}
		c.pc = 0x1000;
		c.run();

		int results = c.a.get();

		logger.debug(String.format("Computed Value = %02x", results));
		assert (results == 0x00);
		assert (c.CFLAG.isSet());

	}
	@Test
	public void Test_ADC_BCD03() {
		try {
			CPU_CreateUtil.load(c.bus, "ADC_DEC_mode3.hex", 0);

		} catch (IOException | illegalAddressException | ROException | DeviceUnavailable e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			fail("Unable to load test program");

		}
		c.pc = 0x1000;
		c.run();

		int results = c.a.get();

		logger.debug(String.format("Computed Value = %02x", results));
		assert (results == 0x10);
		assert (!c.CFLAG.isSet());
		assert (!c.ZFLAG.isSet());
		assert (!c.NFLAG.isSet());

	}
	@Test
	public void Test_ADC_BCD04() {
		try {
			CPU_CreateUtil.load(c.bus, "ADC_DEC_mode4.hex", 0);

		} catch (IOException | illegalAddressException | ROException | DeviceUnavailable e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			fail("Unable to load test program");

		}
		c.pc = 0x1000;
		c.run();

		int results = c.a.get();

		logger.debug(String.format("Computed Value = %02x", results));
		assert (results == 0x98);
		assert (c.CFLAG.isSet());
		assert (!c.ZFLAG.isSet());
		assert (!c.NFLAG.isSet());

	}
}
