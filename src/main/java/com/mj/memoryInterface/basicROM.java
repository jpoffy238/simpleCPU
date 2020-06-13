package com.mj.memoryInterface;

import com.mj.Devices.MemoryRange;
import com.mj.Devices.PBus.BussId;
import com.mj.Devices.PBus.DEVTYPE;
import com.mj.exceptions.ROException;
import com.mj.exceptions.illegalAddressException;

public class basicROM implements MemoryDriver {

	private byte[] ROM = new  byte[0xffff-0xf000];
	public basicROM() {
		
	}
	public DEVTYPE getDeviceType() {
		// TODO Auto-generated method stub
		return DEVTYPE.CHAR;
	}

	public BussId getBusId() {
		// TODO Auto-generated method stub
		return BussId.MEMROY;
	}

	public MemoryRange getAddressRange() {
		// TODO Auto-generated method stub
		return new MemoryRange(0xf000, 0xffff);
	}

	public byte read(int address) throws illegalAddressException {
		// TODO Auto-generated method stub
		return 0;
	}

	public void write(int address, byte data) throws illegalAddressException, ROException {
		// TODO Auto-generated method stub
		throw new ROException();
	}

}
