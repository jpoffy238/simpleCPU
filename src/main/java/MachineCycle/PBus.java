package MachineCycle;

import Devices.Device;
import exceptions.DeviceUnavailable;
import exceptions.illegalAddressException;

public interface PBus {
	 enum BussId  { MEMROY,  DEVICE, INTERUPT, CPU }
	 enum IO {READ_IO, WRITE_IO }
	
	public void write (int address, byte data) throws illegalAddressException, DeviceUnavailable;
	
	public  byte  read ( int destAddress ) throws illegalAddressException, DeviceUnavailable;
	
	public void blockWrite (int address, byte data[]) throws illegalAddressException, DeviceUnavailable;
	
	public  byte[]  blockRead ( int destAddress ) throws illegalAddressException, DeviceUnavailable;
	public void raiseInterupt();
	public void raiseNMInterupt();
	public void raisepowerOnReset();
	
	
	public void  registerDevice (BussId bus, int address, int length, Device deviceHandler );

	public void unregisterDevice(BussId bus, int address);
	
	
}
