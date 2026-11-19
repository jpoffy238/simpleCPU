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

	/** Flattens an Intel HEX file into a plain byte[] spanning exactly its
	 * own lowest through highest DATA address - a real CP/M file (or a
	 * cc65-built .bin) is this shape, not a multi-region hex file itself,
	 * so SDCC's own .ihx output (bdos.ihx/ccp.ihx/edit.ihx/..., unlike
	 * cc65's own already-flat .bin) needs this conversion before
	 * CpmDiskTool's own put() can write it onto a disk image as a usable
	 * file. No bus/device needed at all for this - a pure transform
	 * directly over the parsed records, unlike load() above. */
	public static byte[] flatten(String fileName) throws IOException {
		BasicIntelHexFiles reader = new BasicIntelHexFiles();
		ArrayList<IntelHexRecord> records;
		try {
			records = reader.read(fileName);
		} catch (IntelHexFileChecksumMisMatchException e) {
			throw new IOException(e);
		}

		int lowest = Integer.MAX_VALUE;
		int highest = Integer.MIN_VALUE;
		for (IntelHexRecord r : records) {
			if (r.getRecordType() == IntelHexRecordType.DATA && r.getData().length > 0) {
				lowest = Math.min(lowest, r.getAddress());
				highest = Math.max(highest, r.getAddress() + r.getData().length);
			}
		}
		if (highest <= lowest) {
			throw new IOException(fileName + ": no DATA records found - not a real Intel HEX file?");
		}

		byte[] flat = new byte[highest - lowest];
		for (IntelHexRecord r : records) {
			if (r.getRecordType() == IntelHexRecordType.DATA) {
				byte[] data = r.getData();
				System.arraycopy(data, 0, flat, r.getAddress() - lowest, data.length);
			}
		}
		return flat;
	}
}
