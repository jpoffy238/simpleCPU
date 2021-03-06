package com.mj.IntelHex.common;

import java.io.IOException;
import java.util.ArrayList;

public interface IntelHexFile {
	public ArrayList< IntelHexRecord> read(String filename) throws IOException, IntelHexFileChecksumMisMatchException;
	public int write(String fileName, ArrayList<IntelHexRecord> records);
}
