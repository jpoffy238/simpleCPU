package com.mj.memoryInterface;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.mj.Devices.AddressRange;
import com.mj.Devices.PBus;
import com.mj.Devices.PBus.BussId;
import com.mj.Devices.PBus.DEVTYPE;
import com.mj.Devices.PBus.IOALLOW;
import com.mj.IntelHex.BasicIntelHexFiles;
import com.mj.IntelHex.common.IntelHexFileChecksumMisMatchException;
import com.mj.IntelHex.common.IntelHexRecord;
import com.mj.IntelHex.common.IntelHexRecordType;
import com.mj.exceptions.ROException;
import com.mj.exceptions.illegalAddressException;

public abstract class AbstractMemoryLayer implements MemoryDriver {
	private static final Logger logger = LogManager.getLogger(AbstractMemoryLayer.class);
	private Lock readLock = new ReentrantLock();
	private Lock writeLock = new ReentrantLock();
	protected PBus sysbus;
	protected DEVTYPE dtype = DEVTYPE.CHAR;
	protected BussId busId = BussId.MEMROY;
	protected IOALLOW ioallow = IOALLOW.RW;

	protected AddressRange memrange;
	protected byte[] memory;
	public AbstractMemoryLayer (PBus bus, AddressRange memsize, String OptinalFileToLoad, int startAddress) {
		sysbus = bus;
		this.memrange = memsize;
		memory = new byte[memrange.size()+1];
		
		if (OptinalFileToLoad != null ) {
			try {
				load(OptinalFileToLoad, startAddress);
			} catch (illegalAddressException | ROException | IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
	}
	public DEVTYPE getDeviceType() {
		// TODO Auto-generated method stub
		return dtype;
	}

	@Override
	public BussId getBusId() {
		// TODO Auto-generated method stub
		return busId;
	}
	
	@Override
	public AddressRange getAddressRange() {
		// TODO Auto-generated method stub
		return memrange;
	}

	@Override
	public void raiseInterrupt() {
		// TODO Auto-generated method stub

	}

	public byte read(int address) throws illegalAddressException  {
		// TODO Auto-generated method stub
		if (ioallow != IOALLOW.RO && ioallow != IOALLOW.RW) {
			throw new illegalAddressException(address, 0);
		}
		int value = 0;
		if (memrange.contains(address)) {
			int localAddress = addressMapper(address);
			readLock.lock();
			value = (int) (memory[localAddress] & 0xff);
			readLock.unlock();
		} else {
			throw new illegalAddressException(address, 00);
		}
		return (byte) (value & 0xff);

	}
	public void write(int address, byte data) throws illegalAddressException, ROException {
		// TODO Auto-generated method stub
		// For devices the odd address is control while even is data.
		if (ioallow == IOALLOW.RO) {
			throw new ROException();
		}
		if (memrange.contains(address)) {
			writeLock.lock();
			int localAddress = addressMapper(address);
			memory[localAddress] = (byte) (data & 0xff);
			writeLock.unlock();
		} else {
			throw new illegalAddressException(address, data);
		}

	}

	@Override
	public void load(String fileName, int startAddress) throws illegalAddressException, ROException, IOException {
	
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
//						try {
//							sysbus.write(VirtualAddress, program[idx]);
//						} catch (ROException | illegalAddressException | DeviceUnavailable e) {
//							// TODO Auto-generated catch block
//							e.printStackTrace();
//						}
						
						memory[localMemoryAddress] = program[idx];
					}

			

			}
		
	}
}
protected int addressMapper(int virtualAddress) throws  illegalAddressException {
	if (! memrange.contains(virtualAddress)) {
		logger.error(String.format("Unable to Map Address %08x" , virtualAddress));
			
		throw new illegalAddressException(virtualAddress, 0);
	}
		int localAddress = virtualAddress -  memrange.baseAddress() ;
		if ((localAddress < 0 )  || ( localAddress >= memrange.size())) {
			logger.error(String.format("Unable to Map Address %04x to local Adress %04x" , virtualAddress, localAddress));
			throw new illegalAddressException(virtualAddress, localAddress);
		}
	
	return localAddress;
}	
public void reset() {
	
}
}
