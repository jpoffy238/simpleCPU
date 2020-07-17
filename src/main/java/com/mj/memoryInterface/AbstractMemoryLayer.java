package com.mj.memoryInterface;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.mj.Devices.AddressRange;
import com.mj.Devices.PBus.BussId;
import com.mj.Devices.PBus.DEVTYPE;
import com.mj.IntelHex.BasicIntelHexFiles;
import com.mj.IntelHex.common.IntelHexFileChecksumMisMatchException;
import com.mj.IntelHex.common.IntelHexRecord;
import com.mj.IntelHex.common.IntelHexRecordType;
import com.mj.exceptions.ROException;
import com.mj.exceptions.illegalAddressException;

public abstract class AbstractMemoryLayer implements MemoryDriver {
	private Lock readLock = new ReentrantLock();
	private Lock writeLock = new ReentrantLock();

	protected DEVTYPE dtype;
	protected BussId busId;
	protected AddressRange memrange;
	protected byte[] memory;
	public DEVTYPE getDeviceType() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public BussId getBusId() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public AddressRange getAddressRange() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void raiseInterrupt() {
		// TODO Auto-generated method stub

	}

	public byte read(int address) throws illegalAddressException {
		// TODO Auto-generated method stub
		int value = 0;
		if (memrange.contains(address)) {
			readLock.lock();
			value = (int) (memory[address] & 0xff);
			readLock.unlock();
		} else {
			throw new illegalAddressException(address, 00);
		}
		return (byte) (value & 0xff);

	}
	public void write(int address, byte data) throws illegalAddressException, ROException {
		// TODO Auto-generated method stub
		// For devices the odd address is control while even is data.

		if (memrange.contains(address)) {
			writeLock.lock();
			memory[address] = (byte) (data & 0xff);
			writeLock.unlock();
		} else {
			throw new illegalAddressException(address, data);
		}

	}

	@Override
	public void load(String fileName, int startAddress) throws illegalAddressException, ROException {
		int i = startAddress; // program load point
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
						int localMemoryAddress = addressMapper(VirtualAddress);
						memory[localMemoryAddress] = program[idx];
					}

			

			}
		
	}
}
private int addressMapper(int virtualAddress) throws  illegalAddressException {
	if (! memrange.contains(virtualAddress)) {
		throw new illegalAddressException(virtualAddress, 0);
	}
		int localAddress = virtualAddress -  memrange.baseAddress() ;
		if ((localAddress < 0 )  || ( localAddress >= memrange.size())) {
			throw new illegalAddressException(virtualAddress, localAddress);
		}
	
	return localAddress;
}	
}
