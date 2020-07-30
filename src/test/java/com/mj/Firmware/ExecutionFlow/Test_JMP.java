package com.mj.Firmware.ExecutionFlow;

import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.util.ArrayList;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.mj.Devices.AddressRange;
import com.mj.Devices.ConsoleDevice;
import com.mj.Devices.DeviceBus;
import com.mj.Devices.PBus;
import com.mj.Firmware.Framework.cpu001decoder;
import com.mj.IntelHex.BasicIntelHexFiles;
import com.mj.IntelHex.common.IntelHexFileChecksumMisMatchException;
import com.mj.IntelHex.common.IntelHexRecord;
import com.mj.cpu001.CPU;
import com.mj.exceptions.DeviceUnavailable;
import com.mj.exceptions.ROException;
import com.mj.exceptions.illegalAddressException;
import com.mj.memoryInterface.basicMemory;
import com.mj.memoryInterface.basicROM;
import com.mj.util.CPU_CreateUtil;

public class Test_JMP {
	private static CPU c;

	private static final Logger logger = LogManager.getLogger(Test_JMP.class);

	@BeforeAll
	public static void setup() {
		c = CPU_CreateUtil.getCPU();
		logger.debug("In Setup -- Current cpu state: " + CPU.currentThread().getState());
	}

	@Test
	public void Test_JMPABS() {
		try {
		CPU_CreateUtil.load(c.bus, "/home/jpoffen/git/simpleCPU/src/main/asm/JMP_ABS.hex", 0);
		
		} catch (IOException | illegalAddressException | ROException | DeviceUnavailable e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			fail("Unable to load test program");

		}

		

		logger.debug("Starting CPU");
		c.run();

		int result = c.a.get();
		logger.debug("What A is loaded with : ", +result);
		try {
			assert (result == c.bus.read(0x2000));
			assert (result == 11);
			assert (!c.ZFLAG.isSet());
			assert (!c.NFLAG.isSet());
		} catch (illegalAddressException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			assert (false);
		} catch (DeviceUnavailable e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			assert (false);
		}

	}

	@Test
	public void Test_JMPIND() {
		
		try {
			CPU_CreateUtil.load(c.bus, "/home/jpoffen/git/simpleCPU/src/main/asm/JMP_IND.hex", 0);
			
			
		} catch (IOException | illegalAddressException | ROException | DeviceUnavailable e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			fail("Unable to load test program");

		}

		logger.debug("Starting CPU");
		c.run();
		
		int result = c.a.get();
		
		logger.debug("What A is loaded with : ", +result);
		try {
			assert (result == c.bus.read(0x2000));
			assert (result == 11);
			assert (!c.ZFLAG.isSet());
			assert (!c.NFLAG.isSet());
		} catch (illegalAddressException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			assert (false);
		} catch (DeviceUnavailable e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			assert (false);
		}

	}
}
