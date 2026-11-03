package com.mj.Disk;

import java.io.IOException;
import java.io.RandomAccessFile;

import com.mj.Disk.exceptions.DiskReadError;
import com.mj.Disk.exceptions.DiskWriteError;
import com.mj.Disk.exceptions.GeometryException;

/** A disk image stored as a flat, track-ordered file: sector data only, no gap/sync/CRC bytes. */
public class FileVirtualDisk implements VirtualDisk {
	private DiskGeometry geometry;
	private RandomAccessFile file;

	@Override
	public void create(DiskGeometry geometry, String fileName) throws DiskWriteError {
		this.geometry = geometry;
		try {
			file = new RandomAccessFile(fileName, "rw");
			file.setLength(geometry.totalBytes());
		} catch (IOException e) {
			throw new DiskWriteError("Unable to create disk image " + fileName, e);
		}
	}

	@Override
	public void open(DiskGeometry geometry, String fileName) throws DiskReadError {
		this.geometry = geometry;
		try {
			file = new RandomAccessFile(fileName, "rw");
		} catch (IOException e) {
			throw new DiskReadError("Unable to open disk image " + fileName, e);
		}
	}

	@Override
	public byte[] read(int head, int track, int sector) throws DiskReadError, GeometryException {
		checkGeometry(head, track, sector);
		byte[] data = new byte[geometry.getSectorSize()];
		try {
			file.seek(geometry.offset(head, track, sector));
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
			file.seek(geometry.offset(head, track, sector));
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
