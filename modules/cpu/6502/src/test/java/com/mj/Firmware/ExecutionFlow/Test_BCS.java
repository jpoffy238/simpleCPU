package com.mj.Firmware.ExecutionFlow;

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

public class Test_BCS {
	private static CPU c;

	private static final Logger logger = LogManager.getLogger(Test_BCS.class);

	@BeforeAll
	public static void setup() {

	
		c = CPU_CreateUtil.getCPU();
		

		logger.debug("In Setup -- Current cpu state: " + CPU.currentThread().getState());
	}

	@Test
	public void Test_BCS_01() {
		try {

			CPU_CreateUtil.load(c.bus, "BCS_TEST.hex", 0);
		
			} catch (IOException | illegalAddressException | ROException | DeviceUnavailable e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				assert (false);
			}
	

		

		logger.debug("Starting CPU");
		c.run();

		int result = c.a.get();
		logger.debug("What A is loaded with : ", +result);
		try {
			assert (result == c.bus.read(0x2001));

			assert ((char) result == (char) 'C');
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
	public void Test_BCS_02() {
	
		try {

			CPU_CreateUtil.load(c.bus, "BCS_TEST2.hex", 0);
		
			} catch (IOException | illegalAddressException | ROException | DeviceUnavailable e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				assert (false);
			}
	
		
		

		logger.debug("Starting CPU");
		c.run();

		int result = c.a.get();
		logger.debug("What A is loaded with : ", +result);
		try {
			assert (result == c.bus.read(0x2001));

			assert ((char) result == (char) 'N');
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
