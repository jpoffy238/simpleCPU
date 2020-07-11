package org.IntelHex;

import org.IntelHex.common.IntelHexFields;
import org.IntelHex.common.IntelHexFileInvalidFormatException;
import org.IntelHex.common.IntelHexRecord;
import org.IntelHex.common.IntelHexRecordType;

public class BasicIntelHexRecord implements IntelHexRecord {
	// Record structure
	// A record (line of text) consists of six fields (parts) that appear in order
	// from left to right:[7]
	//
	// Start code, one character, an ASCII colon ':'.
	// Byte count, two hex digits (one hex digit pair), indicating the number of
	// bytes (hex digit pairs) in the data field. The maximum byte count is 255
	// (0xFF). 16 (0x10) and 32 (0x20) are commonly used byte counts.
	// Address, four hex digits, representing the 16-bit beginning memory address
	// offset of the data. The physical address of the data is computed by adding
	// this offset to a previously established base address, thus allowing memory
	// addressing beyond the 64 kilobyte limit of 16-bit addresses. The base
	// address, which defaults to zero, can be changed by various types of records.
	// Base addresses and address offsets are always expressed as big endian values.
	// Record type (see record types below), two hex digits, 00 to 05, defining the
	// meaning of the data field.
	// Data, a sequence of n bytes of data, represented by 2n hex digits. Some
	// records omit this field (n equals zero). The meaning and interpretation of
	// data bytes depends on the application.
	// Checksum, two hex digits, a computed value that can be used to verify the
	// record has no errors.
	// 1-2-4-2-2n-2
	// where n is the hex value of field 2

	private byte[] data;
	private int recordLength;
	private int address;
	private IntelHexRecordType type;
	private int checksum;

	public IntelHexRecord parse(String data) throws IntelHexFileInvalidFormatException {
		// TODO Auto-generated method stub
		if ( ':' != data.charAt(IntelHexFields.StartCode.getStart())) {
			throw new IntelHexFileInvalidFormatException(IntelHexFields.StartCode, data);
		} 
		recordLength = AsciiHexToInt(data.substring(IntelHexFields.ByteCount.getStart(),
				IntelHexFields.ByteCount.getEnd() ));
		address = AsciiHexToInt(data.substring(IntelHexFields.Address.getStart(), IntelHexFields.Address.getEnd()));
		
		type = IntelHexRecordType.search(AsciiHexToInt(
				data.substring(IntelHexFields.RecordType.getStart(),
						IntelHexFields.RecordType.getEnd() )));
		
		
		return this;
	}

	@Override
	public int getRcordLength() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void setRecordLength(int len) {
		// TODO Auto-generated method stub

	}

	@Override
	public int getAddress() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void setAddress(int address) {
		// TODO Auto-generated method stub

	}

	@Override
	public IntelHexRecordType getRecordType() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public byte[] getData() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setData(byte[] data) {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean Validate() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void createChecksum() {
		// TODO Auto-generated method stub

	}

	@Override
	public void setRecordType(IntelHexRecordType type) {
		// TODO Auto-generated method stub

	}

	private byte asciiHexTobyteNibble(char hexChar) {
		byte returnValue = 0;
		if (Character.isDigit(hexChar)) {
			returnValue = (byte) ((byte) (hexChar) - (byte) ('0'));
		}
		if (hexChar >= 'A' && hexChar <= 'F') {
			returnValue = (byte) ((byte) (hexChar) - (byte) ('A'));

		}
		return (byte)(returnValue & (byte)0x0f);
	}
private byte asciiHexToByte(String data) {
	byte returnValue = 0;
	for (int i = 0; i < data.length(); i++) {
		returnValue <<= 4; // shift 4 to add in next nibble
		returnValue += asciiHexTobyteNibble(data.charAt(i));
	}
	return returnValue;
}
private int AsciiHexToInt(String data) {
	int returnValue = 0;
	for (int i = 0; i < data.length(); i++) {
		returnValue <<= 4; // shift 4 to add in next nibble
		returnValue += asciiHexTobyteNibble(data.charAt(i));
	}
	return returnValue;
}
}
