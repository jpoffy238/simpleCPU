package com.mj.util;

import java.io.IOException;
import java.util.ArrayList;

import com.mj.Devices.AddressRange;
import com.mj.Devices.ConsoleDevice;
import com.mj.Devices.DeviceBus;
import com.mj.Devices.PBus;
import com.mj.Devices.RTC;
import com.mj.Firmware.Framework.cpu001decoder;
import com.mj.IntelHex.BasicIntelHexFiles;
import com.mj.IntelHex.common.IntelHexFileChecksumMisMatchException;
import com.mj.IntelHex.common.IntelHexRecord;
import com.mj.IntelHex.common.IntelHexRecordType;
import com.mj.cpu001.CPU;
import com.mj.exceptions.DeviceUnavailable;
import com.mj.exceptions.ROException;
import com.mj.exceptions.illegalAddressException;
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
		basicROM rom = new basicROM(bus, 
				new AddressRange(0xf000, 0xffff),
				"RTCInt.hex", 0);
		
		bus.registerDevice(rom);
	
	
	
	
	bus.registerDevice(new ConsoleDevice(bus, new AddressRange(0xe000, 0xe001)));
	bus.registerDevice(rtc);
		
		 return  new CPU(bus, new cpu001decoder());
	}
	
	public static void load(PBus bus, String fileName, int startAddress) throws illegalAddressException, ROException, IOException, DeviceUnavailable {
		
		BasicIntelHexFiles testCode = new BasicIntelHexFiles();
		ArrayList<IntelHexRecord> code = new ArrayList<IntelHexRecord>();
		try {
			code = testCode.read(fileName);
		} catch (IntelHexFileChecksumMisMatchException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			
			
		}
		for (int x = 0; x < code.size(); x++) {
			IntelHexRecord r = code.get(x);
			if (r.getRecordType() == IntelHexRecordType.DATA) {

				byte[] program = r.getData();
				
					for (int idx = 0; idx < program.length; idx++) {
						int VirtualAddress =  startAddress + idx + r.getAddress();
						
					
							bus.write(VirtualAddress, program[idx]);
					
						
	
					}

			

			}
		
	}
}
}
