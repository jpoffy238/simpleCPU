package org.IntelHex.common;

public class IntelHexFileInvalidFormatException extends Exception {
	IntelHexFields BadField;
	public IntelHexFileInvalidFormatException(IntelHexFields field, String message) {
		super(message);
		BadField = field;
	}
	/**
	 * 
	 */
	private static final long serialVersionUID = -7560407509212924679L;

}
