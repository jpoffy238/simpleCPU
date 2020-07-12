package org.IntelHex.common;

public class IntelHexFileChecksumMisMatchException extends Exception {
	/**
	 * 
	 */
	private static final long serialVersionUID = 8635019000554765281L;
	IntelHexFields BadField;
	public IntelHexFileChecksumMisMatchException(IntelHexFields field, String message) {
		super(message);
		BadField = field;
	}
	/**
	 * 
	 */
	

}
