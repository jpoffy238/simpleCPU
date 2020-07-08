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
	private static final Logger logger = LogManager.getLogger(Test_RTC.class);

	@BeforeAll
	public static void setup() {
		PBus bus = new DeviceBus();
		RTC rtc = new RTC(bus);
		RTCstartAddress = 0xa000;
		RTCEndAddress =  RTCstartAddress  +21;
		
		
		
		
		
		bus.registerDevice(new basicMemory());
		bus.registerDevice(new basicROM(bus));
		bus.registerDevice(new FileDevice(bus));
		bus.registerDevice(rtc);
		rtc.start();
		
		 c = new CPU(bus, new cpu001decoder());
	
		logger.debug("In Setup -- Current cpu state: " + CPU.currentThread().getState());
	}
	@Test
	public void Test_Reading_RTC() {
		int i = 0x1000;
		byte lowerStartAddress = (byte)(RTCstartAddress&0x00ff);
		byte UpperStartAddress = (byte)((RTCstartAddress >> 8) &0x00ff);
		int MARKER ;
		int START ;
		try {
			c.bus.write(i++,  OpCodes.LDY_IMM.code()); // Setup Y to loop for 0x7f types
			c.bus.write(i++,  (byte)(0x7f));
			START = i;
			c.bus.write(i++, OpCodes.LDX_IMM.code());  // Load lenght into X
			c.bus.write(i++,(byte)(20) );
			MARKER=i; // For branch location
			c.bus.write(i++, OpCodes.LDA_ABSX.code()); // Clock is read backwards 
			c.bus.write(i++, (byte)(0x00));
			c.bus.write(i++,  (byte)(0x20));
			c.bus.write(i++,  OpCodes.STA_ABS.code()); // write to output
			c.bus.write(i++, (byte)(0x00));
			c.bus.write(i++,  (byte)(0xec));
			
			c.bus.write(i++, OpCodes.DEX.code());  // dec x to point to next location
			c.bus.write(i++, OpCodes.BNE.code()); // branch for next			 
			c.bus.write(i++,  (byte) (MARKER - i +1));
			c.bus.write(i++, OpCodes.LDA_ABSX.code()); // Clock is read backwards 
			c.bus.write(i++, (byte)(0x00));
			c.bus.write(i++,  (byte)(0x20));
			c.bus.write(i++,  OpCodes.STA_ABS.code()); // write to output
			c.bus.write(i++, (byte)(0x00));
			c.bus.write(i++,  (byte)(0xec));
			c.bus.write(i++,OpCodes.DEY.code());
			c.bus.write(i++,  OpCodes.BNE.code());
			c.bus.write(i++,  (byte) (START - i +1));
			
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
		logger.debug("Stopped CPU");
		char []  results = new char[21];
		for (i=0; i < 21 ; i++) {
			try {
				results[i] = (char)c.bus.read(0x2000 + i);
				logger.debug("results[" + i + "] = " + results[i]);
			} catch (illegalAddressException | DeviceUnavailable e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		String ouput = new String(results);
		logger.debug("Time  " + ouput);
		

}
}
