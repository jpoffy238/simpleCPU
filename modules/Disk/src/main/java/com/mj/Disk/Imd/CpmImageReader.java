package com.mj.Disk.Imd;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.mj.Disk.exceptions.FileSystemException;

/** Reads files off a real CP/M disk image via ImdImage + a DiskParameterBlock,
 * rather than this project's own fixed-format CpmFileSystem (see that class's own
 * comment for the on-disk layout it assumes - this project's in-house convention,
 * not a universal one). A real period CP/M disk's own reserved-track count, block
 * size, and block-pointer width vary by system (see DiskParameterBlock), so this
 * class takes them as parameters instead of hardcoding this project's own choices.
 * <p>
 * Deliberately a separate class rather than a generalization of CpmFileSystem:
 * that class is load-bearing for every CPU/BDOS test in this project, and bolting
 * configurable DPB fields onto it risks regressing something real for a feature
 * whose only consumer, so far, is reading real external images. Read-only, no
 * writing/formatting - that's CpmFileSystem's own job, for this project's own
 * format.
 * <p>
 * Validated 2026-11-20 against a real TRS-80 Model II CP/M 2.2 8" disk image
 * (DiskParameterBlock.trs80ModelIiCpm22()): correctly lists its real directory
 * (MENU.COM, PIP.COM, ED.COM, SAMPLE.TXT, ...) and reads SAMPLE.TXT back byte for
 * byte - confirmed readable VEDIT documentation text, not noise. */
public class CpmImageReader {
	private static final int STATUS_EMPTY = 0xE5;

	/** Already-deblocked flat bytes (ImdImage.toFlatBytes(), or a caller's own) -
	 * expected to be a whole multiple of DiskParameterBlock.RECORD_SIZE. */
	private final byte[] records;
	private final DiskParameterBlock dpb;

	public CpmImageReader(byte[] records, DiskParameterBlock dpb) {
		this.records = records;
		this.dpb = dpb;
	}

	public CpmImageReader(ImdImage image, DiskParameterBlock dpb) {
		this(image.toFlatBytes(), dpb);
	}

	/** Filtered to each file's first extent (EX == 0), same convention as
	 * CpmFileSystem.listFiles() - without it, a multi-extent file would appear
	 * once per extent. */
	public List<String> listFiles() {
		List<String> names = new ArrayList<String>();
		for (int entry = 0; entry < dpb.directoryEntryCount(); entry++) {
			byte[] raw = readDirectoryEntry(entry);
			if ((raw[0] & 0xff) != STATUS_EMPTY && (raw[12] & 0xff) == 0) {
				names.add(decodeFileName(raw));
			}
		}
		return names;
	}

	/** Returns the file's data, length rounded up to the nearest 128-byte record -
	 * the same real-CP/M convention CpmFileSystem.readFile() documents. */
	public byte[] readFile(String fileName) throws FileSystemException {
		byte[] fcbName = encodeFileName(fileName);
		List<Integer> blocks = new ArrayList<Integer>();
		int totalRecords = 0;
		boolean foundAny = false;

		for (int extent = 0; ; extent++) {
			int index = findEntry(fcbName, extent);
			if (index < 0) {
				break;
			}
			foundAny = true;
			byte[] raw = readDirectoryEntry(index);
			int recordsInExtent = raw[15] & 0xff;
			totalRecords += recordsInExtent;
			for (int i = 0; i < dpb.pointersPerExtent(); i++) {
				int block = blockPointer(raw, i);
				if (block != 0) {
					blocks.add(block);
				}
			}
			if (recordsInExtent < 128) {
				break; // wasn't full - there's no extent after this one
			}
		}
		if (!foundAny) {
			throw new FileSystemException("File not found: " + fileName);
		}

		int blockBytes = dpb.recordsPerBlock() * DiskParameterBlock.RECORD_SIZE;
		byte[] result = new byte[totalRecords * DiskParameterBlock.RECORD_SIZE];
		int copied = 0;
		for (int block : blocks) {
			if (copied >= result.length) {
				break;
			}
			int length = Math.min(blockBytes, result.length - copied);
			System.arraycopy(readBlock(block), 0, result, copied, length);
			copied += length;
		}
		return result;
	}

	private int findEntry(byte[] fcbName, int extent) {
		for (int entry = 0; entry < dpb.directoryEntryCount(); entry++) {
			byte[] raw = readDirectoryEntry(entry);
			if ((raw[0] & 0xff) != STATUS_EMPTY && (raw[12] & 0xff) == extent && nameMatches(raw, fcbName)) {
				return entry;
			}
		}
		return -1;
	}

	private byte[] readDirectoryEntry(int index) {
		int offset = dpb.reservedRecords() * DiskParameterBlock.RECORD_SIZE + index * DiskParameterBlock.ENTRY_SIZE;
		byte[] entry = new byte[DiskParameterBlock.ENTRY_SIZE];
		System.arraycopy(records, offset, entry, 0, DiskParameterBlock.ENTRY_SIZE);
		return entry;
	}

	private byte[] readBlock(int block) {
		int startByte = (dpb.reservedRecords() + block * dpb.recordsPerBlock()) * DiskParameterBlock.RECORD_SIZE;
		int length = dpb.recordsPerBlock() * DiskParameterBlock.RECORD_SIZE;
		byte[] result = new byte[length];
		System.arraycopy(records, startByte, result, 0, length);
		return result;
	}

	private int blockPointer(byte[] entry, int index) {
		int base = 16 + index * dpb.blockPointerWidth();
		int value = entry[base] & 0xff;
		if (dpb.blockPointerWidth() == 2) {
			value |= (entry[base + 1] & 0xff) << 8;
		}
		return value;
	}

	private boolean nameMatches(byte[] rawEntry, byte[] fcbName) {
		for (int i = 0; i < 11; i++) {
			if (rawEntry[1 + i] != fcbName[i]) {
				return false;
			}
		}
		return true;
	}

	/** Package-private (not private) so test fixtures can build directory entries
	 * using the exact same 8.3 encoding this class reads back, rather than
	 * duplicating the encoding logic. */
	static byte[] encodeFileName(String fileName) throws FileSystemException {
		String name = fileName;
		String ext = "";
		int dot = fileName.indexOf('.');
		if (dot >= 0) {
			name = fileName.substring(0, dot);
			ext = fileName.substring(dot + 1);
		}
		if (name.isEmpty() || name.length() > 8 || ext.length() > 3) {
			throw new FileSystemException("Invalid file name (max 8.3 characters): " + fileName);
		}
		byte[] fcbName = new byte[11];
		Arrays.fill(fcbName, (byte) ' ');
		byte[] nameBytes = name.toUpperCase().getBytes(StandardCharsets.US_ASCII);
		byte[] extBytes = ext.toUpperCase().getBytes(StandardCharsets.US_ASCII);
		System.arraycopy(nameBytes, 0, fcbName, 0, nameBytes.length);
		System.arraycopy(extBytes, 0, fcbName, 8, extBytes.length);
		return fcbName;
	}

	private String decodeFileName(byte[] rawEntry) {
		String name = new String(rawEntry, 1, 8, StandardCharsets.US_ASCII).trim();
		String ext = new String(rawEntry, 9, 3, StandardCharsets.US_ASCII).trim();
		return ext.isEmpty() ? name : name + "." + ext;
	}
}
