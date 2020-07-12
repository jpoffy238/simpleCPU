package org.IntelHex.common;

public enum IntelHexRecordType {
	DATA(0), // DATA - Contains data and a 16-bit starting address for the data.
	EOF(1), // End Of File -- Must occur exactly once per file in the last line of the file.
	ESA(2), // Extended Segment Address -- The data field contains a 16-bit segment
			// base address (thus byte count is always 02) compatible with 80x86 real mode
			// addressing.
	SSA(3), // Start Segment Address -- For 80x86 processors, specifies the initial content
			// of the CS:IP registers (i.e., the starting execution address).
	ELA(4), // Extended Linear Address --Allows for 32 bit addressing (up to 4GiB)
	SLA(5) // Start Linear Address -- he address field is 0000 (not used) and the byte
			// count is always 04.
			// The four data bytes represent a 32-bit address value (big-endian). In the
			// case of
			// 80386 and higher CPUs, this address is loaded into the EIP register.
	;

	int value;
	
	private IntelHexRecordType(int rt) {
		value = rt;
	
	}
	public static  IntelHexRecordType search(int i) throws IntelHexFileInvalidFormatException {
		IntelHexRecordType  returnValue = null;
		switch (i) {
		case 0: returnValue = DATA;
			break;
		case 1 : returnValue = EOF;
		break;
		case 2 : returnValue = ESA;
		break;
		case 3 : returnValue = SSA;
		break;
		case 4 : returnValue = ELA;
		break;
		case 5 : returnValue = SLA;
		break;
		default : throw new IntelHexFileInvalidFormatException(IntelHexFields.RecordType, "Invalid Record Type ");
		}
		return returnValue;
	}
	public int valueOf() {
		return value;
	}
	
}
