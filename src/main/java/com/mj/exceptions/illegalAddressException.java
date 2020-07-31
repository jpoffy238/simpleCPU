package com.mj.exceptions;

public class illegalAddressException extends SoCException {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -3659977671301131208L;

	public illegalAddressException(int address, int data) {
		super(String.format("IllegalAddress : %x04  Data value %x04", address, data));
		
	}

}
