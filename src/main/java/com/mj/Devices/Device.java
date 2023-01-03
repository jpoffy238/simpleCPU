package com.mj.Devices;


import com.mj.exceptions.DeviceOperationNotImpl;
import com.mj.exceptions.DeviceUnavailable;
import com.mj.exceptions.ROException;
import com.mj.exceptions.illegalAddressException;

public interface Device  {
	

	enum DEVTYPE {
		BLOCK, CHAR
	}

	enum IOALLOW {
		RO, RW, WO
	}
	public DEVTYPE getDeviceType();

	public IOALLOW geIOAllow();
	
	public void write(int offset, byte data) throws illegalAddressException, ROException, DeviceUnavailable;

	public byte read(int offset) throws illegalAddressException, DeviceUnavailable;

	public void blockWrite( int offset, byte data[], int blocksize)
			throws ROException, illegalAddressException, DeviceUnavailable, DeviceOperationNotImpl;

	public byte[] blockRead(int offset, int blocksize) throws illegalAddressException, DeviceUnavailable, DeviceOperationNotImpl;

	public void raiseInterrupt();

	public void reset();
	public void start();
	public void stop();
	

	

}
