package org.IntelHex.common;

public interface IntelHexRecord {
	public IntelHexRecord parse(String data) throws IntelHexFileInvalidFormatException, IntelHexFileChecksumMisMatchException;
	public int getRcordLength();
	public void setRecordLength(int len);
	public int getAddress();
	public void setAddress(int address);
	public IntelHexRecordType getRecordType();
	public void  setRecordType(IntelHexRecordType type);
	public byte[] getData();
	public void setData(byte[] data);
	public boolean Validate();
	public void createChecksum();
}
