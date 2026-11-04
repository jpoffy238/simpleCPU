package com.mj.Firmware.FileDevice;


import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.util.ArrayList;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.mj.IntelHex.BasicIntelHexFiles;
import com.mj.IntelHex.common.IntelHexFileChecksumMisMatchException;
import com.mj.IntelHex.common.IntelHexRecord;
import com.mj.IntelHex.common.IntelHexRecordType;
import com.mj.cpu001.CPU;
import com.mj.exceptions.DeviceUnavailable;
import com.mj.exceptions.ROException;
import com.mj.exceptions.illegalAddressException;
import com.mj.util.CPU_CreateUtil;

public class Test_RTC {
	private static CPU c;
	
	private static final Logger logger = LogManager.getLogger(Test_RTC.class);
	
	@BeforeAll
	public static void setup() {
		c = CPU_CreateUtil.getCPU();
	
		logger.debug("In Setup -- Current cpu state: " + CPU.currentThread().getState());
	}
	@Test
	public void Test_Reading_RTC() {
		int i = 0x1000; // program load point
		
		BasicIntelHexFiles testCode = new BasicIntelHexFiles();
		ArrayList<IntelHexRecord> code = new ArrayList<IntelHexRecord>();
		try {
			code = testCode.read("RTC_TEST.hex");
		} catch (IOException | IntelHexFileChecksumMisMatchException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			fail("Unable to load test program");
		}
		for (int x = 0; x < code.size(); x++) {
			IntelHexRecord r = code.get(x);
			if (r.getRecordType() == IntelHexRecordType.DATA) {

				byte[] program = r.getData();
				try {
					for (int idx = 0; idx < program.length; idx++) {
						c.bus.write(i + idx + r.getAddress(), program[idx]);
					}

				} catch (illegalAddressException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					assert (false);
				} catch (DeviceUnavailable e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					logger.error(String.format("Unable to access address %04x" ,e.getAddress()));
				} catch (ROException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					assert (false);
				}

			}
		}
		int before = (int) (System.currentTimeMillis() / 1000L);
		logger.debug("Starting CPU");
		c.start();
		c.bus.startDevices();


		try {
			Thread.sleep(2000);

		} catch ( InterruptedException e1) {
			// TODO Auto-generated catch block

		}
		c.bus.stopDevices();
		c.interrupt();
		logger.debug("CPU Terminated");
		int after = (int) (System.currentTimeMillis() / 1000L);

		int uTime = 0;
		for (i = 0; i < 4; i++) {
			try {
				int b = c.bus.read(0x4000 + i) & 0xff;
				uTime = (uTime << 8) | b;
			} catch (illegalAddressException | DeviceUnavailable e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		logger.debug(String.format("RTC u_time copied by RTCInt: %d (wall clock %d..%d)", uTime, before, after));

		// The program runs for ~2 seconds of wall-clock time, so the copy may land
		// anywhere between the epoch second measured before and after the run.
		assertTrue(uTime >= before && uTime <= after,
				"RTC copy " + uTime + " should be between " + before + " and " + after);

}
}
