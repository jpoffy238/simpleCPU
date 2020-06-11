package com.mj.MachineCycle;

import com.mj.Devices.Device;
import com.mj.exceptions.DeviceUnavailable;
import com.mj.exceptions.illegalAddressException;

public interface PBus {
	 enum BussId  { MEMROY,  DEVICE, INTERUPT, CPU }
	 enum DEVTYPE {BLOCK, CHAR }
	
	public void write (BussId bus, int address, byte data) throws illegalAddressException, DeviceUnavailable;
	
	public  byte  read (BussId bus,  int destAddress ) throws illegalAddressException, DeviceUnavailable;
	
	public void blockWrite (BussId bus, int address, byte data[]) throws illegalAddressException, DeviceUnavailable;
	
	public  byte[]  blockRead ( BussId bus, int destAddress ) throws illegalAddressException, DeviceUnavailable;
	public void raiseInterupt();
	public void raiseNMInterupt();
	public void raisepowerOnReset();
	
	
	public void  registerDevice (BussId bus, DEVTYPE t,  int address, int length, Device deviceHandler );

	public void unregisterDevice(BussId bus, DEVTYPE t , int address);
	
	
}
