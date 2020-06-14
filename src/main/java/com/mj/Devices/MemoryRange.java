package com.mj.Devices;

public class MemoryRange  implements  Comparable<MemoryRange> {
	protected int startAddressRange;
	protected int  EndAddressRange;
	protected int length;
	
	public MemoryRange(int saddress, int eaddress) {
		startAddressRange = saddress;
		EndAddressRange = eaddress;
		length = EndAddressRange - startAddressRange +1;
	}
	public boolean  contains(int address) {
		// TODO Auto-generated method stub
		return ((startAddressRange <= address)  && (address <= EndAddressRange ));
	}
	public int size() {
		return (length);
	}
	public int compareTo(MemoryRange other) {
		// TODO Auto-generated method stub
		if (other == null) {
            throw new NullPointerException("Cannot compare to null.");
        }
        if (this == other) {
            return 0;
        }
        Integer thisStartAddr = this.startAddressRange;
        Integer thatStartAddr = other.startAddressRange;
        return thisStartAddr.compareTo(thatStartAddr);
	
	}
	public String toString() {
		return  String.format("Start Address %04x : End Address %04x ", startAddressRange, EndAddressRange); 
	} //

}
