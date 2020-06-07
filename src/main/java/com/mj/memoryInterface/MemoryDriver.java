package com.mj.memoryInterface;

import com.mj.Devices.Device;
import com.mj.exceptions.DeviceUnavailable;
import com.mj.exceptions.illegalAddressException;

public interface MemoryDriver {
	public byte read(int address) throws illegalAddressException, DeviceUnavailable;
	public void write (int address, byte data) throws illegalAddressException, DeviceUnavailable ;
	
	public void setIOPage(int address);
	public int getIOPage();

	public void registerDevice(int address, Device d);
	public void unregisterDevice(int address);
}
