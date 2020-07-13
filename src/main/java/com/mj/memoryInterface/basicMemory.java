package com.mj.memoryInterface;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.mj.Devices.MemoryRange;
import com.mj.Devices.PBus.BussId;
import com.mj.Devices.PBus.DEVTYPE;
import com.mj.Devices.PBus.IOALLOW;
import com.mj.exceptions.ROException;
import com.mj.exceptions.illegalAddressException;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class basicMemory implements MemoryDriver {
	private Lock readLock = new ReentrantLock();
	private Lock writeLock = new ReentrantLock();

	final Logger logger = LogManager.getLogger("basicMemory");
	final BussId bus = BussId.MEMROY;
	final DEVTYPE type = DEVTYPE.CHAR;
	final IOALLOW ioallow = IOALLOW.RW;
	public final static int maxMemorySize = 32 * 1024;;
	final MemoryRange range = new MemoryRange(0, maxMemorySize);

	private byte[] memory;

	public basicMemory() {
		memory = new byte[range.size() + 1];
	}

	public byte read(int address) throws illegalAddressException {
		// TODO Auto-generated method stub
		int value = 0;
		if (range.contains(address)) {
			readLock.lock();
			value = (int) (memory[address] & 0xff);
			readLock.unlock();
		} else {
			throw new illegalAddressException(address, 00);
		}
		return (byte) (value & 0xff);

	}

	

	public void write(int address, byte data) throws illegalAddressException {
		// TODO Auto-generated method stub
		// For devices the odd address is control while even is data.

		if (range.contains(address)) {
			writeLock.lock();
			memory[address] = (byte) (data & 0xff);
			writeLock.unlock();
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

	public void raiseInterrupt() {
		// TODO Auto-generated method stub

	}

	@Override
	public void load(String fileName, int startAddress) throws illegalAddressException, ROException {
		// TODO Auto-generated method stub

	}

}
