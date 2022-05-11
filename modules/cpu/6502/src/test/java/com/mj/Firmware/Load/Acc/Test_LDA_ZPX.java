package com.mj.Firmware.Load.Acc;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.mj.Firmware.Framework.OpCodes;
import com.mj.cpu001.CPU;
import com.mj.exceptions.DeviceUnavailable;
import com.mj.exceptions.ROException;
import com.mj.exceptions.illegalAddressException;
import com.mj.util.CPU_CreateUtil;

public class Test_LDA_ZPX {
	private static CPU c;
	
	private static final Logger logger = LogManager.getLogger("Test_LDA_ZPX");

	@BeforeAll
	public static void setup() {
		c = CPU_CreateUtil.getCPU();
		logger.debug("In Setup -- Current cpu state: " + CPU.currentThread().getState());
	}
	@Test
	public void Test_exec() {
		int i = 0x1000;
		try {
			c.bus.write(i++,  OpCodes.LDX_IMM.code());
			c.bus.write(i++,  (byte)0x10);
			c.bus.write(i++, OpCodes.LDA_ZX.code());
			c.bus.write(i++, (byte) (0x04));
			c.bus.write(i++, OpCodes.HLT.code());
			c.bus.write(0x0013,(byte)0x01);
			c.bus.write(0x0014, (byte) 0x55);
			c.bus.write(0x0015,(byte)0xff);
		} catch (illegalAddressException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (DeviceUnavailable e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ROException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		logger.debug("Starting CPU");
		c.run();
		
		int result = c.a.get();
		logger.debug("What A is loaded with : ", +result);
		try {
			assert (result == c.bus.read(0x14));
			assert(result != c.bus.read(0x13));
			assert(result != c.bus.read(0x15));
		} catch (illegalAddressException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (DeviceUnavailable e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}


}
