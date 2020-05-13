package memoryInterface;

public interface MemoryDriver {
	public byte read(int address);
	public void write (int address, byte data);
	public void blockWrite(int startAddress, int length, byte[] data);
	public byte[] blockRead(int startAddress, int length);
	public void setIOPage(int address );
	public int getIOPage();
	
}
