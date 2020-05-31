package memoryInterface;

import exceptions.DeviceUnavailable;
import exceptions.illegalAddressException;

public interface MemoryDriver {
	public byte read(int address) throws illegalAddressException, DeviceUnavailable;
	public void write (int address, byte data) throws illegalAddressException, DeviceUnavailable ;
	
	public void setIOPage(int address);
	public int getIOPage();

	
}
