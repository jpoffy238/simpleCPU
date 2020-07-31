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
		bus.registerDevice(new basicROM(bus, 
				new AddressRange(0xf000, 0xffff),
				"/home/jpoffen/git/simpleCPU/src/main/asm/RTCInt.hex", 0xf000));
	
	
	
	
	bus.registerDevice(new ConsoleDevice(bus));
	bus.registerDevice(rtc);
		
		 return  new CPU(bus, new cpu001decoder());
	}
	
	public static void load(PBus bus, String fileName, int startAddress) throws illegalAddressException, ROException, IOException, DeviceUnavailable {
		
		BasicIntelHexFiles testCode = new BasicIntelHexFiles();
		ArrayList<IntelHexRecord> code = new ArrayList<IntelHexRecord>();
		try {
			code = testCode.read(fileName);
		} catch (IOException | IntelHexFileChecksumMisMatchException e) {
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
