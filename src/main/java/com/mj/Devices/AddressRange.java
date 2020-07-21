package com.mj.Devices;

import com.mj.exceptions.illegalAddressException;

public class AddressRange  implements  Comparable<AddressRange> {
	protected int startAddressRange;
	protected int  EndAddressRange;
	protected int length;
	
	public AddressRange(int saddress, int eaddress) {
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
	public int compareTo(AddressRange other) {
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
	public int baseAddress() {
		return startAddressRange;
	}
	public String toString() {
		return  String.format("Start Address %04x : End Address %04x ", startAddressRange, EndAddressRange); 
	} //
	
	public int addressMapper(int virtualAddress) throws  illegalAddressException {
		// 
		if (! contains(virtualAddress)) {
			throw new illegalAddressException(virtualAddress, 0);
		}
			int localAddress = virtualAddress - baseAddress() ;
			if ((localAddress < 0 )  || ( localAddress >= size())) {
				throw new illegalAddressException(virtualAddress, localAddress);
			}
		
		return localAddress;
	}	
}
