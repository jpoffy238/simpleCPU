package org.IntelHex.common;

public enum IntelHexFields {
	StartCode(0, 1),
	ByteCount(1, 2),
	Address(3, 4),
	RecordType(7,2),
	Data(9, -1),
	checksum(-1, -1);
	
	private final int start;
	private final int length;
	private final int end;
	private IntelHexFields(int start, int length) {
		this.start = start;
		this.length = length;
		this.end = start + length;
	}
	public  int getStart() {
		return start;
	}
	public  int getLength () {
		return length;
	}
	public int getEnd() {
		return end;
	}
}
