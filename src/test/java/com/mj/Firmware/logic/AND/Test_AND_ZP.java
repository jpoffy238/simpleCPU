package com.mj.Firmware.logic.AND;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.mj.Devices.AddressRange;
import com.mj.Devices.ConsoleDevice;
import com.mj.Devices.DeviceBus;
import com.mj.Devices.PBus;
import com.mj.Firmware.Framework.OpCodes;
import com.mj.Firmware.Framework.cpu001decoder;
import com.mj.cpu001.CPU;
import com.mj.exceptions.DeviceUnavailable;
import com.mj.exceptions.ROException;
import com.mj.exceptions.illegalAddressException;
import com.mj.memoryInterface.basicMemory;
import com.mj.memoryInterface.basicROM;
import com.mj.util.CPU_CreateUtil;

public class Test_AND_ZP {
	private static CPU c;

	private static final Logger logger = LogManager.getLogger(Test_AND_ZP.class);

	@BeforeAll
	public static void setup() {

		c = CPU_CreateUtil.getCPU();
		logger.debug("In Setup -- Current cpu state: " + CPU.currentThread().getState());
	}

	@Test
	public void Test_exec() {
		int i = 0x1000;
		try {
			c.bus.write(i++, OpCodes.LDA_ABS.code());
			c.bus.write(i++, (byte) 0x00);
			c.bus.write(i++, (byte) (0x20));
			c.bus.write(i++,OpCodes.LDX_IMM.code() );
			c.bus.write(i++, (byte)0x01);
			c.bus.write(i++, OpCodes.AND_ZP.code());
			c.bus.write(i++, (byte) 0x01);
			
			c.bus.write(i++, OpCodes.HLT.code());
			//data
			c.bus.write(0x1fff, (byte) 0x00);
			c.bus.write(0x2000, (byte) 0x55);
			c.bus.write(0x2001, (byte) 0xaa);
			c.bus.write(0x0001,(byte)(0xaa));
			
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
		
		int result = c.a.get();
		logger.debug("What A is loaded with : ", +result);

		assert (result == 0);
		assert (c.ZFLAG.isSet());
		assert(!c.NFLAG.isSet());

	}

}
