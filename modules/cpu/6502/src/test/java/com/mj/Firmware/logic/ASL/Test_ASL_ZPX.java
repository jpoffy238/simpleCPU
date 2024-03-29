package com.mj.Firmware.logic.ASL;

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

public class Test_ASL_ZPX {
	private static CPU c;

	private static final Logger logger = LogManager.getLogger(Test_ASL_ZPX.class);

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
			c.bus.write(i++, (byte)(0x20));
			c.bus.write(i++, OpCodes.ASL_ZPX.code());
			c.bus.write(i++,(byte)(0x04));
			
			c.bus.write(i++, OpCodes.HLT.code());
			c.bus.write(0x1fff, (byte) 0x00);
			c.bus.write(0x2000, (byte) 0x55);
			c.bus.write(0x2001, (byte) 0xaa);
			c.bus.write(0x0024, (byte)0x44);
			
		} catch (illegalAddressException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			assert (false);
		} catch (DeviceUnavailable e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			assert (false);
		} catch (ROException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			assert (false);
		}
		try {
			assert(c.bus.read(0x0024) == 0x44);
		} catch (illegalAddressException | DeviceUnavailable e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		logger.debug("Starting CPU");
		c.run();
	
		try {
			int initialValue = (int)(c.bus.read(0x0024) & 0xff);
			logger.debug("ASL_ZPX: " + initialValue);
			assert (initialValue == 0x0088);
			assert(c.NFLAG.isSet());
			assert(!c.ZFLAG.isSet());
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
