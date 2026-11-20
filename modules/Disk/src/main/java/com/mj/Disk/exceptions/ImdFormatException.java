package com.mj.Disk.exceptions;

/** The bytes being parsed aren't a well-formed ImageDisk (.IMD) file, or use a
 * feature ImdImage doesn't support yet (a variable-length sector-size table, or a
 * sector cylinder/head map - both real but rare IMD features, used for duplicated/
 * relocated tracks no real image this project has tried actually needs). */
public class ImdFormatException extends Exception {

	private static final long serialVersionUID = 1L;

	public ImdFormatException(String message) {
		super(message);
	}

}
