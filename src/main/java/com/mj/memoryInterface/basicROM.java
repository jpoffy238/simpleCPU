package com.mj.memoryInterface;

import java.io.IOException;

import com.mj.Devices.AddressRange;
import com.mj.Devices.PBus;
import com.mj.Devices.PBus.IOALLOW;
import com.mj.exceptions.DeviceUnavailable;
import com.mj.exceptions.ROException;
import com.mj.exceptions.illegalAddressException;
import com.mj.util.CPU_CreateUtil;

public class basicROM extends AbstractMemoryLayer {

	
	public basicROM(PBus bus, AddressRange memsize, String OptinalFileToLoad, int startAddress) {
		super (bus, memsize, OptinalFileToLoad, startAddress);
		
		try {
			bus.write(0xfffa, (byte)0);
			bus.write(0xfffb, (byte)0x10);
			bus.write(0xfffc, (byte)0);
			bus.write(0xfffd, (byte)0x10);
			bus.write(0xfffe, (byte)0);
			bus.write(0xffff, (byte)0xf0);

			// load the RTC interrupt handler 
			CPU_CreateUtil.load(bus, "/home/jpoffen/git/simpleCPU/src/main/asm/RTCInt.hex", 0x000);
		} catch (illegalAddressException | ROException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (DeviceUnavailable e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		ioallow = IOALLOW.RO;


	}

	@Override
	public void write(int address, byte data) throws illegalAddressException, ROException {
		// TODO Auto-generated method stub
		if (ioallow != IOALLOW.RO) {
		memory[addressMapper(address)] = data;
		}else {
		throw new ROException();
	}
	}

	public void raiseInterrupt() {
		// TODO Auto-generated method stub
		sysbus.raisepowerOnReset();

	}

}
