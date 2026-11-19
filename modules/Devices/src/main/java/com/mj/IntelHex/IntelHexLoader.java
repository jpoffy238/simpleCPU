package com.mj.IntelHex;

import java.io.IOException;
import java.util.ArrayList;

import com.mj.Devices.PBus;
import com.mj.IntelHex.common.IntelHexFileChecksumMisMatchException;
import com.mj.IntelHex.common.IntelHexRecord;
import com.mj.IntelHex.common.IntelHexRecordType;
import com.mj.exceptions.DeviceUnavailable;
import com.mj.exceptions.ROException;
import com.mj.exceptions.illegalAddressException;

/** CPU-agnostic "write every DATA record onto a bus" loop - both cc65 (6502)
 * and SDCC (Z80) emit plain Intel HEX, so this is shared rather than
 * duplicated per CPU module. Lives here, not in either cpu/ module, so
 * neither depends on the other just to load a .hex/.ihx file - mirrors
 * BasicIntelHexFiles' own CPU-agnostic placement. */
public class IntelHexLoader {
	public static void load(PBus bus, String fileName, int startAddress)
			throws illegalAddressException, ROException, IOException, DeviceUnavailable {
		BasicIntelHexFiles reader = new BasicIntelHexFiles();
		ArrayList<IntelHexRecord> records;
		try {
			records = reader.read(fileName);
		} catch (IntelHexFileChecksumMisMatchException e) {
			throw new IOException(e);
		}
		for (IntelHexRecord r : records) {
			if (r.getRecordType() == IntelHexRecordType.DATA) {
				byte[] program = r.getData();
				for (int idx = 0; idx < program.length; idx++) {
					int virtualAddress = startAddress + idx + r.getAddress();
					bus.write(virtualAddress, program[idx]);
				}
			}
		}
	}
}
