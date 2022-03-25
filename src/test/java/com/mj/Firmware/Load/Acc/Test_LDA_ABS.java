package com.mj.Firmware.Load.Acc;

import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.mj.cpu001.CPU;
import com.mj.exceptions.DeviceUnavailable;
import com.mj.exceptions.ROException;
import com.mj.exceptions.illegalAddressException;
import com.mj.util.CPU_CreateUtil;

public class Test_LDA_ABS {
	private static CPU c;

	private static final Logger logger = LogManager.getLogger("Test_LDA_ABS");

	@BeforeAll
	public static void setup() {
		
		c = CPU_CreateUtil.getCPU();
		try {
		
			
			CPU_CreateUtil.load(c.bus, "LDA_IMM_TEST.hex", 0);
		} catch (illegalAddressException | ROException | DeviceUnavailable | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			assert(false);
		}
	
		logger.debug("In Setup -- Current cpu state: " + CPU.currentThread().getState());
	}

	@Test
	public void Test_LDA_ABS1() {
	
	
		logger.debug("Starting CPU");
		c.run();
		
		int result = c.a.get();
		logger.debug("What A is loaded with : ", +result);
		try {
			assert (result == c.bus.read(0x2000));
			assert(!c.ZFLAG.isSet());
			assert(!c.NFLAG.isSet());
		} catch (illegalAddressException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			assert(false);
		} catch (DeviceUnavailable e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			assert(false);
		}
		
	}


}
