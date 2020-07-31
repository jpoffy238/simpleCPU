package com.mj.Firmware.FileDevice;


import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.mj.Devices.DeviceBus;
import com.mj.Devices.AddressRange;
import com.mj.Devices.ConsoleDevice;
import com.mj.Devices.PBus;
import com.mj.Devices.RTC;
import com.mj.Firmware.Framework.OpCodes;
import com.mj.Firmware.Framework.cpu001decoder;
import com.mj.IntelHex.BasicIntelHexFiles;
import com.mj.IntelHex.common.IntelHexFileChecksumMisMatchException;
import com.mj.IntelHex.common.IntelHexRecord;
import com.mj.IntelHex.common.IntelHexRecordType;
import com.mj.cpu001.CPU;
import com.mj.exceptions.DeviceUnavailable;
import com.mj.exceptions.ROException;
import com.mj.exceptions.illegalAddressException;
import com.mj.memoryInterface.basicMemory;
import com.mj.memoryInterface.basicROM;
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
			code = testCode.read("/home/jpoffen/git/simpleCPU/src/main/asm/RTC_TEST.hex");
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
		final String DATE_FORMAT_NOW = "yyyy-MM-dd HH:mm";

		Calendar cal = Calendar.getInstance();
		SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT_NOW);
		String tmp = sdf.format(cal.getTime());
		logger.debug("Starting CPU");
		c.start();
		c.bus.startDevices();
		int retry=0;
		
		try {
			while ( ((byte)c.bus.read(0x4000) == 0) ) {
				retry++;
				Thread.sleep(100);
				if (retry > 10) {
					break;
				}
				logger.debug(String.format("Found null at %04x - retryCount = %d",  0x4000, retry));
			}
		} catch (illegalAddressException | DeviceUnavailable | InterruptedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	
		logger.debug("CPU Terminated");
		byte []  results = new byte[DATE_FORMAT_NOW.length()];
		for (i=0; i < DATE_FORMAT_NOW.length() ; i++) {
			try {
				results[i] = (byte)c.bus.read(0x4000 + i);
				logger.debug("results[" + i + "] = " + String.format("%02x",results[i]));
			} catch (illegalAddressException | DeviceUnavailable e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		String ouput = new String(results);
		logger.debug("RTC_Time  " + ouput);
		

		logger.debug ( "Comparing RTC " + ouput  + " to " + tmp);
		assert(tmp.equals(ouput));
		
		

}
}
