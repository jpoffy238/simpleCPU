package com.mj.memoryInterface;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.mj.Devices.MemoryRange;
import com.mj.Devices.PBus.BussId;
import com.mj.Devices.PBus.DEVTYPE;
import com.mj.Devices.PBus.IOALLOW;
import com.mj.exceptions.illegalAddressException;

public class basicMemory implements MemoryDriver {
	final  Logger logger = LogManager.getLogger("basicMemory");
	final BussId  bus = BussId.MEMROY;
	final DEVTYPE type = DEVTYPE.CHAR;
	final IOALLOW ioallow = IOALLOW.RW;
	public final static int maxMemorySize = 32 * 1024;;
	final MemoryRange range = new MemoryRange(0,maxMemorySize );

	
	private byte[] memory;

	public basicMemory() {
		memory = new byte[range.size()];
		}

	public byte read(int address) throws  illegalAddressException {
		// TODO Auto-generated method stub
		int value = 0;
		if (range.contains(address)) {
				value = (int) (memory[address] & 0xff);
			} else {
				throw new illegalAddressException(address, 00);
			} 
		return (byte) (value & 0xff);
	}

	public void write(int address, byte data) throws illegalAddressException {
		// TODO Auto-generated method stub
		// For devices the odd address is control while even is data.

			if (range.contains(address)) {
				memory[address] = (byte) (data & 0x00ff);
			} else {
				throw new illegalAddressException(address, data);
			}
		
	}

	public void setDeviceType(DEVTYPE type) {
		// TODO Auto-generated method stub
		
	}

	public DEVTYPE getDeviceType() {
		// TODO Auto-generated method stub
		return type;
	}

	public void setBussId(BussId bus) {
		// TODO Auto-generated method stub
		
	}

	public BussId getBusId() {
		// TODO Auto-generated method stub
		return bus;
	}

	public void setAddressRange(MemoryRange range) {
		// TODO Auto-generated method stub
		
	}

	public MemoryRange getAddressRange() {
		// TODO Auto-generated method stub
		return range;
	}

	

	

}
