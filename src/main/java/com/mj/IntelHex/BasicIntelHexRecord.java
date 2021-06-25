package com.mj.IntelHex;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.mj.IntelHex.common.IntelHexFields;
import com.mj.IntelHex.common.IntelHexFileChecksumMisMatchException;
import com.mj.IntelHex.common.IntelHexFileInvalidFormatException;
import com.mj.IntelHex.common.IntelHexRecord;
import com.mj.IntelHex.common.IntelHexRecordType;

public class BasicIntelHexRecord implements IntelHexRecord {
	private static final Logger logger = LogManager.getLogger(BasicIntelHexRecord.class);
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
	private IntelHexFields currentField ;
	public IntelHexRecord parse(String data) throws IntelHexFileInvalidFormatException, IntelHexFileChecksumMisMatchException {
		// TODO Auto-generated method stub
		logger.trace("Data to parse" + data);
		currentField = IntelHexFields.StartCode;
		if ( ':' != data.charAt(IntelHexFields.StartCode.getStart())) {
			throw new IntelHexFileInvalidFormatException(IntelHexFields.StartCode, data);
		} 
		currentField = IntelHexFields.ByteCount;
		recordLength = (AsciiHexToInt(data.substring(IntelHexFields.ByteCount.getStart(),
				IntelHexFields.ByteCount.getEnd() ))) & 0x00ff;
		logger.trace("data length = " + String.format("%02x [%d]", recordLength, recordLength));
		currentField = IntelHexFields.Address;
		address = AsciiHexToInt(data.substring(IntelHexFields.Address.getStart(), IntelHexFields.Address.getEnd()));
		logger.trace(String.format("Address = %04x ",address));
		currentField = IntelHexFields.RecordType;
		type = IntelHexRecordType.search(AsciiHexToInt(
				data.substring(IntelHexFields.RecordType.getStart(),
						IntelHexFields.RecordType.getEnd() )));
		logger.trace(String.format("Type = %02x",type.valueOf()));
		
		int dataEnd = recordLength*2 +  IntelHexFields.Data.getStart();
		int checksumStart = dataEnd;
		this.data = getData(data, IntelHexFields.Data.getStart(), dataEnd );
		String debugFormat="%02x" ;
		String forlogger = new String();
		for (int i = 0 ; i < this.data.length; i++) {
			forlogger += String.format(debugFormat,this.data[i]);
		}
		logger.debug("DATA [" + forlogger + "]");
		checksum = (asciiHexToByte(data.substring(checksumStart,checksumStart+2)) & 0x00ff);
		logger.debug(String.format("checksum = %02x",checksum));
		if (Validate()) {
		return this;
		} else {
			throw new IntelHexFileChecksumMisMatchException(IntelHexFields.checksum, "Checksum Mismatch");
		}
	}

	@Override
	public int getRcordLength() {
		// TODO Auto-generated method stub
		return recordLength;
	}

	@Override
	public void setRecordLength(int len) {
		// TODO Auto-generated method stub
		recordLength = len;
	}

	@Override
	public int getAddress() {
		// TODO Auto-generated method stub
		return address;
	}

	@Override
	public void setAddress(int address) {
		// TODO Auto-generated method stub
		this.address = address;
	}

	@Override
	public IntelHexRecordType getRecordType() {
		// TODO Auto-generated method stub
		return type;
	}

	@Override
	public byte[] getData() {
		// TODO Auto-generated method stub
		return data;
	}

	@Override
	public void setData(byte[] data) {
		// TODO Auto-generated method stub
		this.data = data;
	}
	
	@Override
	public boolean Validate() {
		// TODO Auto-generated method stub
		/*
		 * A record's checksum byte is the two's complement of the least significant byte (LSB) 
		 * of the sum of all decoded byte values in the record preceding the checksum. It is computed 
		 * by summing the decoded byte values and extracting the LSB of the sum (i.e., the data checksum), 
		 * and then calculating the two's complement of the LSB (e.g., by inverting its bits and adding one).
		 * 
		 * For example, in the case of the record :0300300002337A1E, the sum of the decoded byte values 
		 * is 03 + 00 + 30 + 00 + 02 + 33 + 7A = E2, which has LSB value E2. The two's complement of E2 is 1E, 
		 * which is the checksum byte appearing at the end of the record.
		 * The validity of a record can be checked by computing its checksum and verifying that the computed 
		 * checksum equals the checksum appearing in the record; an error is indicated if the checksums differ.
		 *  Since the record's checksum byte is the two's complement — and therefore the additive inverse — 
		 *  of the data checksum, this process can be reduced to summing all decoded byte values, including 
		 *  the record's checksum, and verifying that the LSB of the sum is zero. When applied to the preceding 
		 *  example, this method produces the following result: 03 + 00 + 30 + 00 + 02 + 33 + 7A + 1E = 100, 
		 *  which has LSB value 00.
		 */
		int checksum = 0;
		checksum += address & 0x00ff; // Lower byte
		checksum += ((address >> 8) & 0x00ff);  // upper byte
		checksum += recordLength & 0x00ff; // assume single byte
		checksum += type.valueOf() & 0x00ff;
		for (int i = 0; i < data.length ; i++) {
			checksum += data[i];
		}
		int twocomp = (( (checksum ^0xffff) + 1) & 0x00ff);
		logger.trace(String.format("Computed twos complements checksum = %02x", twocomp));
		
		// checksum += this.checksum;
		checksum &= 0x00ff;
		logger.trace(String.format("Computed checksum = %02x", checksum));
		
		if   (( ((byte)checksum + (byte)twocomp) & 0x00ff) == 0 ) {
			return true;
		} else {
		return false;
	}
	}

	@Override
	public void createChecksum() {
		// TODO Auto-generated method stub

	}

	@Override
	public void setRecordType(IntelHexRecordType type) {
		// TODO Auto-generated method stub
		this.type= type;
	}

	private byte asciiHexTobyteNibble(char hexChar) throws IntelHexFileInvalidFormatException {
		byte returnValue = 0;
		boolean validDigit = false;
//		logger.debug("input char to decode = " + hexChar);
		if (Character.isDigit(hexChar)) {
			returnValue = (byte) ((byte) (hexChar) - (byte) ('0'));
			validDigit = true;
		}
		if (hexChar >= 'A' && hexChar <= 'F') {
			returnValue = (byte) ((byte) (hexChar) - (byte) ('A') + 10);
			validDigit = true;

		}
		if (!validDigit) {
			throw new IntelHexFileInvalidFormatException(currentField, "Invalid Hex Digit");
		}
	//	logger.debug(String.format("Ouput Value = %02x",returnValue));
		return (byte)(returnValue & (byte)0x0f);
	}
private byte asciiHexToByte(String data) throws IntelHexFileInvalidFormatException {
	byte returnValue = 0;
	for (int i = 0; i < data.length(); i++) {
		returnValue <<= 4; // shift 4 to add in next nibble
		returnValue += asciiHexTobyteNibble(data.charAt(i));
	}
	return returnValue;
}
private int AsciiHexToInt(String data) throws IntelHexFileInvalidFormatException {
	int returnValue = 0;
	if (data.length() == 4) {
		String upperByte = data.substring(0, 2);
		String LowerByte = data.substring(2, 4);
		logger.trace("Upper = " + upperByte + " Lower = " + LowerByte);
		int upperValue = (int)(asciiHexToByte(upperByte) & 0x00ff);
		int lowerValue = (int)(asciiHexToByte(LowerByte) & 0x00ff);
		returnValue = (upperValue << 8)&0x0000ffff;
		returnValue += lowerValue;
	} else {
		if (data.length() == 2) {
			returnValue = asciiHexToByte(data);
		} else {
			throw new IntelHexFileInvalidFormatException(currentField, "Missing data");
		}
	}
	
	return returnValue;
}
private byte[] getData(String data, int start, int end) throws IntelHexFileInvalidFormatException {
	
	
	byte[] returnValue = new byte[recordLength];
		for (int i = 0; i < returnValue.length; i++ ) {
			String tmp = data.substring(start + i*2 , start + i*2 + 2);
			returnValue[i] = asciiHexToByte(tmp );
		}
	return returnValue;
}
public String toString() {
	// Format :Data Size[2]Address[4]RecordType[2]Data[N]Checksum[2]
	String format = "+%02x%02x%02x%02x";
	String dformat = "%02x";
	int lowerAddress = address & 0x00ff;
	int upperAddress = (address >> 8) & 0x00ff;
	String returnString =  String.format(format, recordLength,lowerAddress,upperAddress,type.valueOf());
	if (data.length != recordLength) {
		String formate = "Data length and recordlength do not match arrysize = %d, message recordSize = %d ";
		logger.error(formate, data.length, recordLength);
	}
	for (int i = 0; i < data.length; i++ ) {
		returnString += String.format(dformat, data[i]);
	}
	returnString += String.format(dformat,checksum);
	return returnString.toUpperCase();
}
}
