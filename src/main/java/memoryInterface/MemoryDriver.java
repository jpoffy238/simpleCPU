package memoryInterface;

import exceptions.DeviceUnavailable;
import exceptions.illegalAddressException;

public interface MemoryDriver {
	public byte read(int address) throws illegalAddressException, DeviceUnavailable;
	public void write (int address, byte data) throws illegalAddressException, DeviceUnavailable ;
	public void blockWrite(int startAddress, int length, byte[] data)  throws illegalAddressException;
	public byte[] blockRead(int startAddress, int length) throws illegalAddressException;
	public void setIOPage(int address, int length );
	public int getIOStartPage();
	public int getLenIOPage();
	
}
