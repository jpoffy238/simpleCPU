package com.mj.Disk.exceptions;

public class DiskWriteError extends Exception {

	private static final long serialVersionUID = -6638513035552079453L;

	public DiskWriteError() {
	}

	public DiskWriteError(String message, Throwable cause) {
		super(message, cause);
	}

}
