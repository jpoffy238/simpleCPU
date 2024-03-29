package com.mj.Firmware.StatusFlags;

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

public class Test_CarryFlag {
	private static CPU c;

	private static final Logger logger = LogManager.getLogger(Test_CarryFlag.class);

	@BeforeAll
	public static void setup() {

		c = CPU_CreateUtil.getCPU();
		logger.debug("In Setup -- Current cpu state: " + CPU.currentThread().getState());
	}

	@Test
	public void Test_FlagClear() {
		int i = 0x1000;
		try {
			c.bus.write(i++,OpCodes.CLC.code() );
			c.bus.write(i++, OpCodes.CLD.code());
			c.bus.write(i++,  OpCodes.CLI.code());
			c.bus.write(i++,  OpCodes.HLT.code())
			;
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
			
		assert (!c.CFLAG.isSet());
		assert (!c.DFLAG.isSet());
		assert(!c.IFLAG.isSet());

	}
	@Test
	public void Test_FlagSet() {
		int i = 0x1000;
		try {
			c.bus.write(i++,OpCodes.SEC.code() );
			c.bus.write(i++, OpCodes.SED.code());
			c.bus.write(i++,  OpCodes.SEI.code());
			c.bus.write(i++,  OpCodes.HLT.code())
			;
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
		
		assert (c.CFLAG.isSet());
		
		assert (c.DFLAG.isSet());
		
		assert(c.IFLAG.isSet());

	}

}
