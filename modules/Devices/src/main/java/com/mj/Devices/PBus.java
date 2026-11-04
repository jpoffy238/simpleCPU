package com.mj.Devices;

import java.util.List;

import com.mj.exceptions.DeviceUnavailable;
import com.mj.exceptions.ROException;
import com.mj.exceptions.illegalAddressException;

public interface PBus extends CPUBus {
	enum BussId {
		MEMROY, DEVICE, INTERUPT, CPU
	}

	enum DEVTYPE {
		BLOCK, CHAR
	}

	enum IOALLOW {
		RO, RW, WO
	}
	public void startDevices();
	public void stopDevices();
	public void write(BussId bus, int address, byte data)
			throws ROException, illegalAddressException, DeviceUnavailable;

	public byte read(BussId bus, int destAddress) throws illegalAddressException, DeviceUnavailable;

	public void blockWrite(BussId bus, int address, byte data[])
			throws ROException, illegalAddressException, DeviceUnavailable;

	public byte[] blockRead(BussId bus, int destAddress) throws illegalAddressException, DeviceUnavailable;

	public void resetDevice();
	
	public void raiseInterupt();

	public void raiseNMInterupt();

	public void raisepowerOnReset();

	public void clearInterupt();

	public void clearNMInterupt();

	public void clearpowerOnReset();

	public void registerDevice(Device deviceHandler);

	public void unregisterDevice(Device deviceHandler);
	
	public List <Device> getDevices();
	

}
