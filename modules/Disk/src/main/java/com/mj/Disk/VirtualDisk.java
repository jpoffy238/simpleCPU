package com.mj.Disk;

import com.mj.Disk.exceptions.DiskReadError;
import com.mj.Disk.exceptions.DiskWriteError;
import com.mj.Disk.exceptions.GeometryException;

/** A disk image backed by a file. Every transfer is addressed explicitly by
 * head/track/sector so callers never have to rely on a prior seek. */
public interface VirtualDisk {
	public void create(DiskGeometry geometry, String fileName) throws DiskWriteError;
	public void open(DiskGeometry geometry, String fileName) throws DiskReadError;
	public byte[] read(int head, int track, int sector) throws DiskReadError, GeometryException;
	public void write(int head, int track, int sector, byte[] data) throws DiskWriteError, GeometryException;
	public void sync() throws DiskWriteError;
}
