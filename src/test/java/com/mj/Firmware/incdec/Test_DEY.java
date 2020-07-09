package com.mj.Firmware.incdec;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

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

public class Test_DEY {
	private static CPU c;

	private static final Logger logger = LogManager.getLogger("Test_LDA_ABS");

	@BeforeAll
	public static void setup() {
		
		PBus bus = new DeviceBus();
		bus.registerDevice(new basicMemory());
		bus.registerDevice(new basicROM(bus));
		
		 c = new CPU(bus, new cpu001decoder());
	
		logger.debug("In Setup -- Current cpu state: " + CPU.currentThread().getState());
	}

	@Test
	public void Test_DEY_01() {
		int i = 0x1000;
		try {
			c.bus.write(i++,  OpCodes.SEI.code());
			c.bus.write(i++, OpCodes.LDY_IMM.code());
			c.bus.write(i++, (byte) (0x01));
			c.bus.write(i++, OpCodes.DEY.code());
			c.bus.write(i++, OpCodes.HLT.code());
			
		} catch (illegalAddressException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			assert(false);
		} catch (DeviceUnavailable e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			assert(false);
		} catch (ROException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			assert(false);
		}
		logger.debug("Starting CPU");
		c.run();
		
		int result = c.y.get();
		logger.debug("What A is loaded with : ", +result);
		assert (result == 0x00);
		assert(c.ZFLAG.isSet());
		assert(!c.NFLAG.isSet());
		
	}


}
