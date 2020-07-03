package com.mj.Firmware.FileDevice;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.mj.Devices.DeviceBus;
import com.mj.Devices.FileDevice;
import com.mj.Devices.PBus;
import com.mj.Devices.RTC;
import com.mj.Firmware.Framework.OpCodes;
import com.mj.Firmware.Framework.cpu001decoder;
import com.mj.cpu001.CPU;
import com.mj.exceptions.DeviceUnavailable;
import com.mj.exceptions.ROException;
import com.mj.exceptions.illegalAddressException;
import com.mj.memoryInterface.basicMemory;
import com.mj.memoryInterface.basicROM;

public class Test_RTC {
	private static CPU c;
	private static  int RTCstartAddress;
	private static int RTCEndAddress;
	private static int STDOUT;
	private static final Logger logger = LogManager.getLogger("Test_FileDevice");

	@BeforeAll
	public static void setup() {
		RTC rtc = new RTC();
		RTCstartAddress = 0xec02;
		RTCEndAddress =   0xec22;
		STDOUT = 0xe000;
		
		rtc.start();
		
		PBus bus = new DeviceBus();
		bus.registerDevice(new basicMemory());
		bus.registerDevice(new basicROM(bus));
		bus.registerDevice(new FileDevice(bus));
		bus.registerDevice(rtc);
		
		
		 c = new CPU(bus, new cpu001decoder());
	
		logger.debug("In Setup -- Current cpu state: " + CPU.currentThread().getState());
	}
	@Test
	public void Test_Reading_RTC() {
		int i = 0x1000;
		int MARKER ;
		try {
			c.bus.write(i++, OpCodes.LDX_IMM.code());
			c.bus.write(i++,(byte)(RTCEndAddress -RTCstartAddress) );
			MARKER=i;
			c.bus.write(i++, OpCodes.LDA_ABSX.code());
			c.bus.write(i++, (byte) (RTCstartAddress&0x00ff));
			c.bus.write(i++, (byte) ((RTCstartAddress >> 8) &0x00ff));
			c.bus.write(i++,  OpCodes.STA_ABS.code());
			c.bus.write(i++, (byte)(0x00));
			c.bus.write(i++,  (byte)(0xec));
	
			c.bus.write(i++, OpCodes.DEX.code());
			c.bus.write(i++, OpCodes.BNE.code());			
			c.bus.write(i++,  (byte) (MARKER - i - 1));
			
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
	
		int result = c.a.get();
		logger.debug("What A is loaded with : ", +result);
		

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
