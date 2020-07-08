package com.mj.Devices;

import com.mj.exceptions.DeviceUnavailable;
import com.mj.exceptions.ROException;
import com.mj.exceptions.illegalAddressException;

public interface PBus extends CPUBus {
	 enum BussId  { MEMROY,  DEVICE, INTERUPT, CPU }
	 enum DEVTYPE {BLOCK, CHAR }
	 enum IOALLOW {RO, RW, WO}
	
	public void write (BussId bus, int address, byte data) throws ROException, illegalAddressException, DeviceUnavailable;
	
	public  byte  read (BussId bus,  int destAddress ) throws illegalAddressException, DeviceUnavailable;
	
	public void blockWrite (BussId bus, int address, byte data[]) throws ROException, illegalAddressException, DeviceUnavailable;
	
	public  byte[]  blockRead ( BussId bus, int destAddress ) throws illegalAddressException, DeviceUnavailable;
	
	public void raiseInterupt();
	
	public void raiseNMInterupt();
	
	public void raisepowerOnReset();
	
	
	public void  registerDevice ( Device deviceHandler );

	public void unregisterDevice(Device deviceHandler);
	
	
}
