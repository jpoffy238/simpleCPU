package com.mj.Firmware.Stack;

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

public class Test_PHA {
	private static CPU c;

	private static final Logger logger = LogManager.getLogger(Test_PHA.class);

	@BeforeAll
	public static void setup() {

		PBus bus = new DeviceBus();
		bus.registerDevice(new basicMemory());
		bus.registerDevice(new basicROM(bus));

		c = new CPU(bus, new cpu001decoder());

		logger.debug("In Setup -- Current cpu state: " + CPU.currentThread().getState());
	}

	@Test
	public void Test_exec() {
		int i = 0x1000;
		try {
			c.bus.write(i++, OpCodes.LDA_ABS.code());
			c.bus.write(i++, (byte) 0x00);
			c.bus.write(i++, (byte) (0x20));
			c.bus.write(i++, OpCodes.PHA.code());
			c.bus.write(i++,OpCodes.LDA_ABS.code());
			c.bus.write(i++, (byte) 0x01);
			c.bus.write(i++, (byte) (0x20));
			c.bus.write(i++, OpCodes.STA_ZP.code());
			c.bus.write(i++, (byte)(0x30));
			c.bus.write(i++,OpCodes.PLA.code());
			c.bus.write(i++, OpCodes.HLT.code());
			c.bus.write(0x1fff, (byte) 0x00);
			c.bus.write(0x2000, (byte) 0x55);
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

		
	
		int result = 0;
		int result1 = 0;
		try {
			result = (int)(c.a.get() & 0x00ff);
			result1 = (int)(c.bus.read(0x30) & 0x00ff);
		} catch (illegalAddressException | DeviceUnavailable e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		logger.debug(String.format("A = [%2x] ZP(30) = [%2x]", result, result1));
		assert (result == 0x55);
		assert (result1 == 0xaa);

	}

}
