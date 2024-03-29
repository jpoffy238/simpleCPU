package com.mj.Firmware.logic.ROR;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.mj.Firmware.Framework.OpCodes;
import com.mj.cpu001.CPU;
import com.mj.exceptions.DeviceUnavailable;
import com.mj.exceptions.ROException;
import com.mj.exceptions.illegalAddressException;
import com.mj.util.CPU_CreateUtil;

public class Test_ROR_ZP {
	private  CPU c;

	private static final Logger logger = LogManager.getLogger(Test_ROR_ZP.class);

	@BeforeEach
	public  void setup() {

		c = CPU_CreateUtil.getCPU();
		logger.debug("In Setup -- Current cpu state: " + CPU.currentThread().getState());
	}

	@Test
	public void Test_ROL_ZP_1() {
		int i = 0x1000;
		try {
			c.bus.write(i++, OpCodes.ROR_ZP.code());
			c.bus.write(i++, (byte) (0x20));
			c.bus.write(i++, OpCodes.HLT.code());
			c.bus.write(0x1fff, (byte) 0x00);
			c.bus.write(0x0020, (byte) 0x55);
			c.bus.write(0x2001, (byte) 0xaa);
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

		logger.debug("Starting CPU");
		c.run();
		
		int result = -1;
		try {
			result = c.bus.read(0x0020);
		} catch (illegalAddressException | DeviceUnavailable e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			assert(false);
		}
		logger.debug("What A is loaded with : ", +result);

		assert ((result & 0xff) == 0x2a);
		assert (!c.ZFLAG.isSet());
		assert(!c.NFLAG.isSet());

	}
	@Test
	public void Test_ROL_ZP_CarrySet() {
		int i = 0x1000;
		try {
			c.bus.write(i++,  OpCodes.SEC.code());
			c.bus.write(i++, OpCodes.ROR_ZP.code());
			c.bus.write(i++, (byte) (0x20));
			c.bus.write(i++, OpCodes.HLT.code());
		
			c.bus.write(0x0020, (byte) 0x55);
			

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

		logger.debug("Starting CPU");
		c.run();
		
		int result = -1;
		try {
			result = c.bus.read(0x0020);
		} catch (illegalAddressException | DeviceUnavailable e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			assert(false);
		}
		logger.debug("What A is loaded with : ", +result);

		assert ((result & 0xff) == 0xaa);
		assert (!c.ZFLAG.isSet());
		assert(c.NFLAG.isSet());
		assert(!c.CFLAG.isSet());

	}

}
