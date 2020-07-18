package com.mj.Firmware.FileDevice;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.mj.Devices.DeviceBus;
import com.mj.Devices.AddressRange;
import com.mj.Devices.ConsoleDevice;
import com.mj.Devices.PBus;
import com.mj.Firmware.Framework.OpCodes;
import com.mj.Firmware.Framework.cpu001decoder;
import com.mj.cpu001.CPU;
import com.mj.exceptions.DeviceUnavailable;
import com.mj.exceptions.ROException;
import com.mj.exceptions.illegalAddressException;
import com.mj.memoryInterface.basicMemory;
import com.mj.memoryInterface.basicROM;

public class Test_FileDevice {
	private static CPU c;

	private static final Logger logger = LogManager.getLogger("Test_FileDevice");

	@BeforeAll
	public static void setup() {
		
		PBus bus = new DeviceBus();
		bus.registerDevice(new basicMemory(bus,
				new AddressRange(0, 32*1024) ,
				null, 0	));
	
	
	bus.registerDevice(new basicROM(bus, 
			new AddressRange(0xff00, 0xffff),
			null, 0));
	bus.registerDevice(new ConsoleDevice(bus));
		
		
		 c = new CPU(bus, new cpu001decoder());
	
		logger.debug("In Setup -- Current cpu state: " + CPU.currentThread().getState());
	}
	@Test
	public void Test_LDA_ABS1() {
		int i = 0x1000;
		try {
			c.bus.write(i++, OpCodes.LDA_ABS.code());
			c.bus.write(i++, (byte) 0x00);
			c.bus.write(i++, (byte) (0x20));
			c.bus.write(i++,  OpCodes.STA_ABS.code());
			c.bus.write(i++, (byte)(0x00));
			c.bus.write(i++,  (byte)(0xec));
			c.bus.write(i++,OpCodes.STA_ABS.code());
			c.bus.write(i++,  (byte)0x01);
			c.bus.write(i++,  (byte)(0xec));
			c.bus.write(i++, OpCodes.HLT.code());
			c.bus.write(0x1fff,(byte)0x00);
			c.bus.write(0x2000, (byte) 0x55);
			c.bus.write(0x2001,(byte)0xff);
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
	
		int result = c.a.get();
		logger.debug("What A is loaded with : ", +result);
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
/*
package com.mj.Firmware.Load.Acc;

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

public class Test_LDA_ABS {
	private static CPU c;

	private static final Logger logger = LogManager.getLogger("Test_LDA_ABS");

	@BeforeAll
	public static void setup() {
		
		PBus bus = new DeviceBus();
		bus.registerDevice(new basicMemory());
		bus.registerDevice(new basicROM());
		
		 c = new CPU(bus, new cpu001decoder());
	
		logger.debug("In Setup -- Current cpu state: " + CPU.currentThread().getState());
	}

	@Test
	public void Test_LDA_ABS1() {
		int i = 0x1000;
		try {
			c.bus.write(i++, OpCodes.LDA_ABS.code());
			c.bus.write(i++, (byte) 0x00);
			c.bus.write(i++, (byte) (0x20));
			c.bus.write(i++, OpCodes.HLT.code());
			c.bus.write(0x1fff,(byte)0x00);
			c.bus.write(0x2000, (byte) 0x55);
			c.bus.write(0x2001,(byte)0xff);
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
		int result = c.a.get();
		logger.debug("What A is loaded with : ", +result);
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
*/
