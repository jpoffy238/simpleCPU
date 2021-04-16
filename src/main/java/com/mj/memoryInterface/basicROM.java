package com.mj.memoryInterface;

import com.mj.Devices.AddressRange;
import com.mj.Devices.PBus;
import com.mj.Devices.PBus.IOALLOW;
import com.mj.exceptions.ROException;
import com.mj.exceptions.illegalAddressException;

public class basicROM extends AbstractMemoryLayer {

	
	public basicROM(PBus bus, AddressRange memsize, String OptinalFileToLoad, int startAddress) {
		super (bus, memsize, OptinalFileToLoad, startAddress);
		
		
			int localMemoryAddress = 1600*1024-6;
			try {
				localMemoryAddress = addressMapper(0x0000fffa);
			} catch (illegalAddressException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			memory[localMemoryAddress+0] = (byte)0x00;
			memory[localMemoryAddress+1] = (byte)0x10;
			memory[localMemoryAddress+2] = (byte)0x00;
			memory[localMemoryAddress+3] = (byte)0x10;
			memory[localMemoryAddress+4] = (byte)0x00;
			memory[localMemoryAddress+5] = (byte)0xf0;


		
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
