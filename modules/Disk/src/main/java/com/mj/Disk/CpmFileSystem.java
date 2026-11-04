package com.mj.Disk;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.mj.Disk.exceptions.DiskReadError;
import com.mj.Disk.exceptions.DiskWriteError;
import com.mj.Disk.exceptions.FileSystemException;
import com.mj.Disk.exceptions.GeometryException;

/** A CP/M-style filesystem on top of a FloppyDevice: a flat (no subdirectories)
 * directory of 32-byte entries ("FCBs"), block-based allocation, 8.3-ish names.
 * This is the obvious, well-documented, genuinely simple choice for the IBM 3740
 * geometry this project already emulates - that geometry IS the classic CP/M 8"
 * SSSD format.
 *
 * Layout for this geometry (1 head, 76 tracks, 26 sectors/track, 128 bytes/sector):
 *   Track 0                 reserved for boot/system use (see BIOS.asm/MONITOR.asm's
 *                           B command, which already loads track 0 sector 1).
 *   Tracks 1-75              the "data area", addressed in 1KB allocation blocks
 *                           (8 sectors/block). Blocks don't align to tracks (26
 *                           isn't divisible by 8), so a block can span a track
 *                           boundary - readDataSector()/writeDataSector() treat
 *                           the data area as one linear sequence of sectors and
 *                           convert to/from (track, sector) from that index.
 *   Blocks 0-1               the directory: 2KB = 64 entries of 32 bytes.
 *   Blocks 2+                file data.
 *
 * v1 scope, deliberately: files are capped at one "extent" (16 block pointers per
 * directory entry = 16KB max), matching real CP/M's directory entry format but not
 * yet its multi-extent mechanism for bigger files. No delete, no rename, no
 * subdirectories - real CP/M didn't have subdirectories either, but delete/rename
 * are reasonable later additions deliberately left out of this first pass.
 *
 * Record counts (and so file lengths read back via readFile()) are only tracked to
 * the nearest 128-byte record, exactly like real CP/M - a file whose length isn't a
 * multiple of 128 reads back padded with whatever was in its last block's unused
 * tail (zero, for a freshly-formatted disk). Callers needing an exact byte count
 * need their own convention (a length prefix, a terminator byte) - same as real
 * CP/M applications did. */
public class CpmFileSystem {
	public static final int BOOT_TRACKS = 1;
	public static final int SECTORS_PER_BLOCK = 8;
	public static final int DIRECTORY_BLOCKS = 2;
	public static final int DIRECTORY_ENTRIES = 64;
	public static final int ENTRY_SIZE = 32;
	public static final int STATUS_EMPTY = 0xE5;
	public static final int MAX_BLOCKS_PER_FILE = 16;

	private final FloppyDevice drive;
	private final DiskGeometry geometry;
	private final int blockSize;

	public CpmFileSystem(FloppyDevice drive) {
		this.drive = drive;
		this.geometry = drive.getGeometry();
		this.blockSize = SECTORS_PER_BLOCK * geometry.getSectorSize();
	}

	/** Marks every directory entry empty ($E5, the real CP/M "available" marker -
	 * not $00, which is a valid user number and would look like a real file).
	 * Leaves file data blocks untouched; they only matter once a directory entry
	 * references them. */
	public void format() throws DiskReadError, DiskWriteError, GeometryException {
		byte[] emptyEntry = new byte[ENTRY_SIZE];
		Arrays.fill(emptyEntry, (byte) STATUS_EMPTY);
		for (int entry = 0; entry < DIRECTORY_ENTRIES; entry++) {
			writeDirectoryEntry(entry, emptyEntry);
		}
	}

	public void createFile(String fileName, byte[] data)
			throws DiskReadError, DiskWriteError, GeometryException, FileSystemException {
		if (data.length > MAX_BLOCKS_PER_FILE * blockSize) {
			throw new FileSystemException(
					"File too large for a single extent (max " + (MAX_BLOCKS_PER_FILE * blockSize) + " bytes): " + fileName);
		}
		byte[] fcbName = encodeFileName(fileName);
		if (findEntry(fcbName) >= 0) {
			throw new FileSystemException("File already exists: " + fileName);
		}

		int blocksNeeded = (data.length + blockSize - 1) / blockSize;
		Set<Integer> used = usedBlocks();
		List<Integer> allocated = new ArrayList<Integer>();
		for (int block = DIRECTORY_BLOCKS; block < totalBlocks() && allocated.size() < blocksNeeded; block++) {
			if (!used.contains(block)) {
				allocated.add(block);
			}
		}
		if (allocated.size() < blocksNeeded) {
			throw new FileSystemException("Not enough free space for " + fileName);
		}

		int freeEntry = findFreeDirectoryEntry();
		if (freeEntry < 0) {
			throw new FileSystemException("Directory is full");
		}

		for (int i = 0; i < allocated.size(); i++) {
			byte[] blockData = new byte[blockSize];
			int offset = i * blockSize;
			int length = Math.min(blockSize, data.length - offset);
			System.arraycopy(data, offset, blockData, 0, length);
			writeBlock(allocated.get(i), blockData);
		}

		byte[] fcb = new byte[ENTRY_SIZE];
		fcb[0] = 0; // user 0
		System.arraycopy(fcbName, 0, fcb, 1, 11);
		fcb[12] = 0; // extent - always 0 in this single-extent-only version
		fcb[15] = (byte) Math.min(128, (data.length + 127) / 128); // 128-byte records used
		for (int i = 0; i < allocated.size(); i++) {
			fcb[16 + i] = (byte) (allocated.get(i).intValue());
		}
		writeDirectoryEntry(freeEntry, fcb);
	}

	/** Returns the file's data, length rounded up to the nearest 128-byte record -
	 * see the class comment on record-count-based lengths. */
	public byte[] readFile(String fileName) throws DiskReadError, GeometryException, FileSystemException {
		byte[] fcbName = encodeFileName(fileName);
		int entryIndex = findEntry(fcbName);
		if (entryIndex < 0) {
			throw new FileSystemException("File not found: " + fileName);
		}
		byte[] raw = readDirectoryEntry(entryIndex);
		int recordCount = raw[15] & 0xff;
		byte[] result = new byte[recordCount * 128];
		int copied = 0;
		for (int i = 0; i < 16 && copied < result.length; i++) {
			int block = raw[16 + i] & 0xff;
			if (block == 0) {
				break;
			}
			byte[] blockData = readBlock(block);
			int length = Math.min(blockSize, result.length - copied);
			System.arraycopy(blockData, 0, result, copied, length);
			copied += length;
		}
		return result;
	}

	public List<String> listFiles() throws DiskReadError, GeometryException {
		List<String> names = new ArrayList<String>();
		for (int entry = 0; entry < DIRECTORY_ENTRIES; entry++) {
			byte[] raw = readDirectoryEntry(entry);
			if ((raw[0] & 0xff) != STATUS_EMPTY) {
				names.add(decodeFileName(raw));
			}
		}
		return names;
	}

	private int findEntry(byte[] fcbName) throws DiskReadError, GeometryException {
		for (int entry = 0; entry < DIRECTORY_ENTRIES; entry++) {
			byte[] raw = readDirectoryEntry(entry);
			if ((raw[0] & 0xff) != STATUS_EMPTY && nameMatches(raw, fcbName)) {
				return entry;
			}
		}
		return -1;
	}

	private int findFreeDirectoryEntry() throws DiskReadError, GeometryException {
		for (int entry = 0; entry < DIRECTORY_ENTRIES; entry++) {
			byte[] raw = readDirectoryEntry(entry);
			if ((raw[0] & 0xff) == STATUS_EMPTY) {
				return entry;
			}
		}
		return -1;
	}

	private Set<Integer> usedBlocks() throws DiskReadError, GeometryException {
		Set<Integer> used = new HashSet<Integer>();
		for (int b = 0; b < DIRECTORY_BLOCKS; b++) {
			used.add(b);
		}
		for (int entry = 0; entry < DIRECTORY_ENTRIES; entry++) {
			byte[] raw = readDirectoryEntry(entry);
			if ((raw[0] & 0xff) == STATUS_EMPTY) {
				continue;
			}
			for (int i = 0; i < 16; i++) {
				int block = raw[16 + i] & 0xff;
				if (block != 0) {
					used.add(block);
				}
			}
		}
		return used;
	}

	private int totalBlocks() {
		return (geometry.getTracks() - BOOT_TRACKS) * geometry.getSectors() / SECTORS_PER_BLOCK;
	}

	private byte[] readDirectoryEntry(int entryIndex) throws DiskReadError, GeometryException {
		int byteOffset = entryIndex * ENTRY_SIZE;
		int sectorIndex = byteOffset / geometry.getSectorSize();
		int offsetInSector = byteOffset % geometry.getSectorSize();
		byte[] sector = readDataSector(sectorIndex);
		byte[] entry = new byte[ENTRY_SIZE];
		System.arraycopy(sector, offsetInSector, entry, 0, ENTRY_SIZE);
		return entry;
	}

	private void writeDirectoryEntry(int entryIndex, byte[] entry)
			throws DiskReadError, DiskWriteError, GeometryException {
		int byteOffset = entryIndex * ENTRY_SIZE;
		int sectorIndex = byteOffset / geometry.getSectorSize();
		int offsetInSector = byteOffset % geometry.getSectorSize();
		byte[] sector = readDataSector(sectorIndex);
		System.arraycopy(entry, 0, sector, offsetInSector, ENTRY_SIZE);
		writeDataSector(sectorIndex, sector);
	}

	private byte[] readBlock(int block) throws DiskReadError, GeometryException {
		byte[] result = new byte[blockSize];
		int baseSectorIndex = block * SECTORS_PER_BLOCK;
		int sectorSize = geometry.getSectorSize();
		for (int i = 0; i < SECTORS_PER_BLOCK; i++) {
			byte[] sector = readDataSector(baseSectorIndex + i);
			System.arraycopy(sector, 0, result, i * sectorSize, sectorSize);
		}
		return result;
	}

	private void writeBlock(int block, byte[] data) throws DiskReadError, DiskWriteError, GeometryException {
		int baseSectorIndex = block * SECTORS_PER_BLOCK;
		int sectorSize = geometry.getSectorSize();
		for (int i = 0; i < SECTORS_PER_BLOCK; i++) {
			byte[] sector = new byte[sectorSize];
			System.arraycopy(data, i * sectorSize, sector, 0, sectorSize);
			writeDataSector(baseSectorIndex + i, sector);
		}
	}

	/** dataSectorIndex is 0-based within the data area - index 0 is the first
	 * sector right after the reserved boot track(s). */
	private byte[] readDataSector(int dataSectorIndex) throws DiskReadError, GeometryException {
		int sectorsPerTrack = geometry.getSectors();
		drive.setTrack(BOOT_TRACKS + dataSectorIndex / sectorsPerTrack);
		drive.setSector(1 + dataSectorIndex % sectorsPerTrack);
		return drive.read();
	}

	private void writeDataSector(int dataSectorIndex, byte[] data) throws DiskWriteError, GeometryException {
		int sectorsPerTrack = geometry.getSectors();
		drive.setTrack(BOOT_TRACKS + dataSectorIndex / sectorsPerTrack);
		drive.setSector(1 + dataSectorIndex % sectorsPerTrack);
		drive.write(data);
	}

	private byte[] encodeFileName(String fileName) throws FileSystemException {
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

	private boolean nameMatches(byte[] rawEntry, byte[] fcbName) {
		for (int i = 0; i < 11; i++) {
			if (rawEntry[1 + i] != fcbName[i]) {
				return false;
			}
		}
		return true;
	}

	private String decodeFileName(byte[] rawEntry) {
		String name = new String(rawEntry, 1, 8, StandardCharsets.US_ASCII).trim();
		String ext = new String(rawEntry, 9, 3, StandardCharsets.US_ASCII).trim();
		return ext.isEmpty() ? name : name + "." + ext;
	}
}
