package com.mj.Devices;

public class MemoryRange  {
	int startAddressRange;
	int  EndAddressRange;
	
	public MemoryRange(int saddress, int eaddress) {
		startAddressRange = saddress;
		EndAddressRange = eaddress;
	}
	public boolean  contains(int address) {
		// TODO Auto-generated method stub
		return ((startAddressRange <= address)  && (address <= EndAddressRange ));
	}
	public int size() {
		return (EndAddressRange - startAddressRange);
	}

}
