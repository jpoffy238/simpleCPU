package com.mj.Disk.exceptions;

public class DiskReadError extends Exception {

	private static final long serialVersionUID = -6638513035552079453L;

	public DiskReadError() {
	}

	public DiskReadError(String message) {
		super(message);
	}

	public DiskReadError(String message, Throwable cause) {
		super(message, cause);
	}

}
