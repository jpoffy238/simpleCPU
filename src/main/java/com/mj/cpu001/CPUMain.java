package com.mj.cpu001;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.mj.Devices.DeviceBus;
import com.mj.Devices.ConsoleDevice;
import com.mj.Devices.PBus;
import com.mj.Firmware.Framework.Decoder;
import com.mj.Firmware.Framework.OpCodes;
import com.mj.Firmware.Framework.cpu001decoder;
import com.mj.memoryInterface.basicMemory;
import com.mj.memoryInterface.basicROM;

public class CPUMain {
	private static final Logger logger = LogManager.getLogger(CPUMain.class);
	public static void main(String[] args) {
			
		PBus bus = new DeviceBus();
		bus.registerDevice(new basicMemory());
		bus.registerDevice(new basicROM(bus));
		bus.registerDevice(new ConsoleDevice(bus));
		Decoder d =  new cpu001decoder();
		
		CPU c = new CPU(bus, d);
		logger.error(String.format("Number of Opcodes = %d ", OpCodes.getMap().size()));
		c.start();
		}
	
	
		
	}


