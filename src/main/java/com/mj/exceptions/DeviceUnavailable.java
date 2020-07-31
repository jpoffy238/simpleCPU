package com.mj.exceptions;

public class DeviceUnavailable extends SoCException {

	/**
	 * 
	 */
	private static final long serialVersionUID = -8516533784492217863L;
	private int address= 0;
	public DeviceUnavailable(int add) {
		address = add;
	}
	public int getAddress() {
		return address;
	}
}
