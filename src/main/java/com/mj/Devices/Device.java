package com.mj.Devices;

import com.mj.Devices.PBus.BussId;
import com.mj.Devices.PBus.DEVTYPE;
import com.mj.exceptions.DeviceUnavailable;
import com.mj.exceptions.ROException;
import com.mj.exceptions.illegalAddressException;

public interface Device  {
	 public DEVTYPE getDeviceType();
	 public BussId getBusId();
	 public MemoryRange getAddressRange();	 
	 public void write(int address, byte data ) throws illegalAddressException, ROException, DeviceUnavailable;
	  public byte read(int address) throws illegalAddressException, DeviceUnavailable;
	  public void raiseInterrupt ();
	  
}