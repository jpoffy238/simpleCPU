package com.mj.Disk;

import java.io.IOException;
import java.io.RandomAccessFile;

import com.mj.Disk.exceptions.DiskReadError;
import com.mj.Disk.exceptions.DiskWriteError;
import com.mj.Disk.exceptions.GeometryException;

/** A disk image stored as a 16-byte self-describing header ("SCPU" magic, heads,
 * tracks, sectors/track, little-endian sector size, 7 reserved bytes) followed by
 * a flat, track-ordered body: sector data only, no gap/sync/CRC bytes. */
public class FileVirtualDisk implements VirtualDisk {
	private static final int HEADER_SIZE = 16;
	private static final byte[] MAGIC = { 'S', 'C', 'P', 'U' };

	private DiskGeometry geometry;
	private RandomAccessFile file;

	@Override
	public void create(DiskGeometry geometry, String fileName) throws DiskWriteError {
		this.geometry = geometry;
		try {
			file = new RandomAccessFile(fileName, "rw");
			// Truncate before re-extending, so re-formatting an existing image actually
			// blanks the body - setLength() alone only zero-fills newly added bytes, not
			// ones that were already there.
			file.setLength(0);
			file.setLength(HEADER_SIZE + geometry.totalBytes());
			writeHeader(geometry);
		} catch (IOException e) {
			throw new DiskWriteError("Unable to create disk image " + fileName, e);
		}
	}

	@Override
	public void open(DiskGeometry geometry, String fileName) throws DiskReadError {
		this.geometry = geometry;
		try {
			file = new RandomAccessFile(fileName, "rw");
			validateHeader(geometry);
		} catch (IOException e) {
			throw new DiskReadError("Unable to open disk image " + fileName, e);
		}
	}

	@Override
	public byte[] read(int head, int track, int sector) throws DiskReadError, GeometryException {
		checkGeometry(head, track, sector);
		byte[] data = new byte[geometry.getSectorSize()];
		try {
			file.seek(HEADER_SIZE + geometry.offset(head, track, sector));
			file.readFully(data);
		} catch (IOException e) {
			throw new DiskReadError(String.format("Read failed at head %d track %d sector %d", head, track, sector), e);
		}
		return data;
	}

	@Override
	public void write(int head, int track, int sector, byte[] data) throws DiskWriteError, GeometryException {
		checkGeometry(head, track, sector);
		try {
			file.seek(HEADER_SIZE + geometry.offset(head, track, sector));
			file.write(data, 0, geometry.getSectorSize());
		} catch (IOException e) {
			throw new DiskWriteError(String.format("Write failed at head %d track %d sector %d", head, track, sector), e);
		}
	}

	@Override
	public void sync() throws DiskWriteError {
		try {
			file.getFD().sync();
		} catch (IOException e) {
			throw new DiskWriteError("Unable to flush disk image to storage", e);
		}
	}

	private void writeHeader(DiskGeometry geometry) throws IOException {
		byte[] header = new byte[HEADER_SIZE];
		System.arraycopy(MAGIC, 0, header, 0, MAGIC.length);
		header[4] = (byte) geometry.getHeads();
		header[5] = (byte) geometry.getTracks();
		header[6] = (byte) geometry.getSectors();
		int sectorSize = geometry.getSectorSize();
		header[7] = (byte) (sectorSize & 0xff);
		header[8] = (byte) ((sectorSize >> 8) & 0xff);
		file.seek(0);
		file.write(header);
	}

	/** Validate-only, not geometry-discovering: the header must match the geometry the
	 * caller (a specific drive type, e.g. IBM3740) already knows it wants. */
	private void validateHeader(DiskGeometry expected) throws DiskReadError, IOException {
		byte[] header = new byte[HEADER_SIZE];
		file.seek(0);
		file.readFully(header);
		for (int i = 0; i < MAGIC.length; i++) {
			if (header[i] != MAGIC[i]) {
				throw new DiskReadError("Not a simpleCPU disk image (bad magic)");
			}
		}
		int heads = header[4] & 0xff;
		int tracks = header[5] & 0xff;
		int sectors = header[6] & 0xff;
		int sectorSize = (header[7] & 0xff) | ((header[8] & 0xff) << 8);
		if (heads != expected.getHeads() || tracks != expected.getTracks()
				|| sectors != expected.getSectors() || sectorSize != expected.getSectorSize()) {
			throw new DiskReadError(String.format(
					"Disk image geometry (heads=%d tracks=%d sectors=%d sectorSize=%d) doesn't match the drive's (heads=%d tracks=%d sectors=%d sectorSize=%d)",
					heads, tracks, sectors, sectorSize,
					expected.getHeads(), expected.getTracks(), expected.getSectors(), expected.getSectorSize()));
		}
	}

	private void checkGeometry(int head, int track, int sector) throws GeometryException {
		if (!geometry.isValidHead(head)) {
			throw new GeometryException("Invalid head " + head);
		}
		if (!geometry.isValidTrack(track)) {
			throw new GeometryException("Invalid track " + track);
		}
		if (!geometry.isValidSector(sector)) {
			throw new GeometryException("Invalid sector " + sector);
		}
	}
}
