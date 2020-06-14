package com.mj.cpu001;

import com.mj.Devices.DeviceBus;
import com.mj.Devices.PBus;
import com.mj.Firmware.Framework.cpu001decoder;
import com.mj.memoryInterface.basicMemory;
import com.mj.memoryInterface.basicROM;

public class CPUMain {

	public static void main(String[] args) {
			
		PBus bus = new DeviceBus();
		bus.registerDevice(new basicMemory());
		bus.registerDevice(new basicROM());
		
		CPU c = new CPU(bus, new cpu001decoder());
		c.start();
		}
	
	
		
	}


