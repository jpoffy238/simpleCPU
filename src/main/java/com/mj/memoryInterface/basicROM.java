package com.mj.memoryInterface;

import java.io.IOException;

import com.mj.Devices.AddressRange;
import com.mj.Devices.PBus;
import com.mj.Devices.PBus.IOALLOW;
import com.mj.exceptions.ROException;
import com.mj.exceptions.illegalAddressException;
import com.mj.Devices.PBus;
import com.mj.Devices.PBus.BussId;
import com.mj.Devices.PBus.DEVTYPE;

public class basicROM extends AbstractMemoryLayer {

	
	public basicROM(PBus bus, AddressRange memsize, String OptinalFileToLoad, int startAddress) {
		super (bus, memsize, OptinalFileToLoad, startAddress);
		
		try {

			memory[addressMapper(0xfffa)] = (byte) (0x00);// Non maskable interrupt handler
			memory[addressMapper(0xfffb)] = (byte) (0xfc);

			memory[addressMapper(0xfffc)] = (byte) (0x00); // Power on reset
			memory[addressMapper(0xfffd)] = (byte) (0x10);

			memory[addressMapper(0xfffe)] = (byte) (0x00); // BRK/interrupt handler
			memory[addressMapper(0xffff)] = (byte) (0xf0);

			// load the RTC interrupt handler 
			load("/home/jpoffen/git/simpleCPU/src/main/asm/RTCInt.hex", 0xf000);
		} catch (illegalAddressException | ROException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		ioallow = IOALLOW.RO;


	}

	@Override
	public void write(int address, byte data) throws illegalAddressException, ROException {
		// TODO Auto-generated method stub
		throw new ROException();
	}

	public void raiseInterrupt() {
		// TODO Auto-generated method stub
		sysbus.raisepowerOnReset();

	}

}
