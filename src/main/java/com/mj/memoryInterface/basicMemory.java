package com.mj.memoryInterface;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.mj.Devices.AddressRange;
import com.mj.Devices.PBus;
import com.mj.Devices.PBus.IOALLOW;

public class basicMemory extends AbstractMemoryLayer  {
	

	final Logger logger = LogManager.getLogger("basicMemory");
	
	final IOALLOW ioallow = IOALLOW.RW;
	
	
	
	public basicMemory(PBus bus, AddressRange memsize, String OptinalFileToLoad, int startAddress) {
		super (bus, memsize, OptinalFileToLoad, startAddress);
	
	}


}
