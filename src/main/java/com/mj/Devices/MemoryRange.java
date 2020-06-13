package com.mj.Devices;

public class MemoryRange  implements  Comparable<MemoryRange> {
	protected int startAddressRange;
	protected int  EndAddressRange;
	
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
	public int compareTo(MemoryRange o) {
		// TODO Auto-generated method stub
		int returnValue = 0;
		if ((startAddressRange == o.startAddressRange) && 
				(EndAddressRange == o.EndAddressRange))  {
			returnValue = 0;
		} else {
			if (  (startAddressRange >= o.EndAddressRange ) || 
				( EndAddressRange <= o.startAddressRange) )
			{
				returnValue = 1;
			} else { 
				if ( ((startAddressRange >= o.startAddressRange ) && (startAddressRange <= o.EndAddressRange ) ) || 
						((EndAddressRange <= o.startAddressRange ) && ( EndAddressRange >= o.EndAddressRange)) ) { 
					returnValue = -1;
				}
			}
		}
		return returnValue;
	}

}
