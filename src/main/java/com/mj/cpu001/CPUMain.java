package com.mj.cpu001;

import com.mj.Devices.BasicBus;
import com.mj.Devices.Device;
import com.mj.Devices.FileDevice;
import com.mj.Firmware.Framework.cpu001decoder;
import com.mj.exceptions.DeviceUnavailable;
import com.mj.exceptions.illegalAddressException;
import com.mj.memoryInterface.MemoryDriver;
import com.mj.memoryInterface.basicMemory;

public class CPUMain {

	public static void main(String[] args) {
			
			
		CPU c = new CPU(new BasicBus(), new cpu001decoder());
		c.start();
		}
	
	
		
	}


