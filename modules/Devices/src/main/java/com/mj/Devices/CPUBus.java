package com.mj.Devices;

import com.mj.exceptions.DeviceUnavailable;
import com.mj.exceptions.ROException;
import com.mj.exceptions.illegalAddressException;

public interface CPUBus  {
		
	public void write(int address, byte data) throws ROException, illegalAddressException, DeviceUnavailable ;
		// TODO Auto-generated method stub
		
	public byte read(int destAddress) throws illegalAddressException, DeviceUnavailable; 

	public boolean IsInterruptRaised();
	public boolean IsNMInterruptRaised();
	public boolean IsPowerOnResetRased();

	
}
