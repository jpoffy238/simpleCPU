package com.mj.Firmware.Load.Acc;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.mj.Devices.BasicBus;
import com.mj.Devices.CPUBus;
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

public class Test_LDX_ABS {
	private static CPU c;
	
	final static Logger logger = LogManager.getLogger("Test_LDA_ABS");

	@BeforeAll
	public static void setup() {
		PBus bus = new DeviceBus();
		bus.registerDevice(new basicMemory());
		bus.registerDevice(new basicROM());
		
		c = new CPU(bus, new cpu001decoder());
	
		logger.debug("In Setup -- Current cpu state: " + CPU.currentThread().getState());
	}



	@Test
	public void Test_LDX_ABS1() {
		int i = 0x1000;
		try {
			c.bus.write(i++, OpCodes.LDX_ABS.code());
			c.bus.write(i++, (byte) 0x00);
			c.bus.write(i++, (byte) (0x20));
			c.bus.write(i++, OpCodes.HLT.code());
			c.bus.write(0x2000, (byte) 0x55);
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
		c.start();
		logger.debug("CPU state = " + c.getState());
		while (c.getState() != Thread.State.TERMINATED) {
			try {
				Thread.sleep(10);
			} catch (Exception e) {

			}
		}
		int result = c.x.get();
		logger.debug("What X is loaded with : ", +result);
		try {
			assert (result == c.bus.read(0x2000));
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
