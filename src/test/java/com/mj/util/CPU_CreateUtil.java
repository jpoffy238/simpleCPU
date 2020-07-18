package com.mj.util;

import com.mj.Devices.AddressRange;
import com.mj.Devices.ConsoleDevice;
import com.mj.Devices.DeviceBus;
import com.mj.Devices.PBus;
import com.mj.Devices.RTC;
import com.mj.Firmware.Framework.cpu001decoder;
import com.mj.cpu001.CPU;
import com.mj.memoryInterface.basicMemory;
import com.mj.memoryInterface.basicROM;

public class  CPU_CreateUtil {
	private static  RTC rtc ;
	public static CPU getCPU() {
		
		PBus bus = new DeviceBus();
		rtc = new RTC(bus);
		
		
		bus.registerDevice(new basicMemory(bus,
				new AddressRange(0, 32*1024) ,
				null, 0	));
	
	
	bus.registerDevice(new basicROM(bus, 
			new AddressRange(0xf000, 0xffff),
			"/home/jpoffen/git/simpleCPU/src/main/asm/RTC_TEST.hex", 0xf000));
	bus.registerDevice(new ConsoleDevice(bus));
	bus.registerDevice(rtc);
		
		 return  new CPU(bus, new cpu001decoder());
	}
}
