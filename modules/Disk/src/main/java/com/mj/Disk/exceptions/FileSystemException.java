package com.mj.Disk.exceptions;

/** Filesystem-level logic errors (not found, duplicate name, disk/directory full,
 * file too large for a single extent) - as opposed to DiskReadError/DiskWriteError,
 * which are I/O failures from the underlying device. */
public class FileSystemException extends Exception {

	private static final long serialVersionUID = 1L;

	public FileSystemException(String message) {
		super(message);
	}

}
