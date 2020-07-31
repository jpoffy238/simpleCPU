package com.mj.memoryInterface;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.mj.Devices.AddressRange;
import com.mj.Devices.PBus;
import com.mj.Devices.PBus.BussId;
import com.mj.Devices.PBus.DEVTYPE;
import com.mj.Devices.PBus.IOALLOW;
import com.mj.exceptions.ROException;
import com.mj.exceptions.illegalAddressException;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class basicMemory extends AbstractMemoryLayer  {
	

	final Logger logger = LogManager.getLogger("basicMemory");
	
	final IOALLOW ioallow = IOALLOW.RW;
	
	
	
	public basicMemory(PBus bus, AddressRange memsize, String OptinalFileToLoad, int startAddress) {
		super (bus, memsize, OptinalFileToLoad, startAddress);
	
	}


}
