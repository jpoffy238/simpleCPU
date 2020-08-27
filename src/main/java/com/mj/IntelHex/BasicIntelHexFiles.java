package com.mj.IntelHex;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

import com.mj.IntelHex.common.IntelHexFile;
import com.mj.IntelHex.common.IntelHexFileChecksumMisMatchException;
import com.mj.IntelHex.common.IntelHexFileInvalidFormatException;
import com.mj.IntelHex.common.IntelHexRecord;

// Record structure
//A record (line of text) consists of six fields (parts) that appear in order from left to right:[7]
//
//Start code, one character, an ASCII colon ':'.
//Byte count, two hex digits (one hex digit pair), indicating the number of bytes (hex digit pairs) in the data field. The maximum byte count is 255 (0xFF). 16 (0x10) and 32 (0x20) are commonly used byte counts.
//Address, four hex digits, representing the 16-bit beginning memory address offset of the data. The physical address of the data is computed by adding this offset to a previously established base address, thus allowing memory addressing beyond the 64 kilobyte limit of 16-bit addresses. The base address, which defaults to zero, can be changed by various types of records. Base addresses and address offsets are always expressed as big endian values.
//Record type (see record types below), two hex digits, 00 to 05, defining the meaning of the data field.
//Data, a sequence of n bytes of data, represented by 2n hex digits. Some records omit this field (n equals zero). The meaning and interpretation of data bytes depends on the application.
//Checksum, two hex digits, a computed value that can be used to verify the record has no errors.
// 1-2-4-2-2n-2
// where n is the hex value of field 2
public class BasicIntelHexFiles implements IntelHexFile {

	
	public ArrayList<IntelHexRecord> read(String filename) throws IOException, IntelHexFileChecksumMisMatchException {
		// TODO Auto-generated method stub
		InputStream inputStream = getClass().getClassLoader().getResourceAsStream(filename);
		
		if (inputStream == null) {
			inputStream = new FileInputStream(new File (filename));
			
		}
		BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
		  ArrayList<IntelHexRecord> records = new ArrayList<IntelHexRecord>();
		  
		  String string; 
		 
		  while ((string = reader.readLine()) != null)  {
			  IntelHexRecord hr = new BasicIntelHexRecord();
			  try {
				hr.parse(string);
			} catch (IntelHexFileInvalidFormatException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			  records.add(hr);
		    System.out.println(string); 
		  } 
		  reader.close();
		  inputStream.close();
		  return records;
		  
	}

	
	public int write(String fileName, ArrayList<IntelHexRecord> records) {
		// TODO Auto-generated method stub
		return -1;
	}

}
