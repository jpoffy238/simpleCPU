package com.mj.memoryInterface;

import com.mj.Devices.Device;
import com.mj.exceptions.DeviceUnavailable;
import com.mj.exceptions.ROException;
import com.mj.exceptions.illegalAddressException;

public interface MemoryDriver extends Device {
	public byte read(int address) throws illegalAddressException;
	public void write (int address, byte data) throws illegalAddressException, ROException;
	

}
