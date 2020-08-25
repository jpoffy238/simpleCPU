package com.mj.cpu001;


import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.mj.Devices.DeviceBus;
import com.mj.Devices.AddressRange;
import com.mj.Devices.ConsoleDevice;
import com.mj.Devices.PBus;
import com.mj.Firmware.Framework.Decoder;
import com.mj.Firmware.Framework.OpCodes;
import com.mj.Firmware.Framework.cpu001decoder;
import com.mj.exceptions.DeviceUnavailable;
import com.mj.exceptions.ROException;
import com.mj.exceptions.illegalAddressException;
import com.mj.memoryInterface.basicMemory;
import com.mj.memoryInterface.basicROM;
import com.mj.util.CPU_CreateUtil;

public class CPUMain {
	private static final Logger logger = LogManager.getLogger(CPUMain.class);
	public static void main(String[] args) {
		
		PBus bus = new DeviceBus();
		bus.registerDevice(new basicMemory(bus,
					new AddressRange(0, 32*1024) ,
					null, 0	));
		
		
		bus.registerDevice(new basicROM(bus, 
				new AddressRange(0xff00, 0xffff),
				null, 0));

		bus.registerDevice(new ConsoleDevice(bus, new AddressRange(0xec00, 0xec01)));
		Decoder d =  new cpu001decoder();
		
		CPU c = new CPU(bus, d);
		logger.error(String.format("Number of Opcodes = %d ", OpCodes.getMap().size()));
		//CPU_CreateUtil.load(bus, fileName, startAddress);
		try {
			CPU_CreateUtil.load(bus,  "Echo.hex", 0);
		} catch (illegalAddressException | ROException | DeviceUnavailable | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		c.start();
		}
	
	
		
	}


