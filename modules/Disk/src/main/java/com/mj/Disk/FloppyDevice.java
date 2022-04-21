package com.mj.Disk;
import com.mj.Disk.exceptions.GeometryException;
import com.mj.Disk.exceptions.DiskReadError;
import com.mj.Disk.exceptions.DiskWriteError;

public interface FloppyDevice {
	public DiskGeometry getGeometry();
	public void setVirutalDiskFile(String fname);
	public void format() throws GeometryException, DiskWriteError;
	public void setHead(int headId) throws GeometryException;
	public void setTrack(int trackId) throws GeometryException;
	public void setSector(int sectorId) throws GeometryException;
	public void spinUp();
	public void spinDown();
	public void loadHead();
	public void unloadHead();
	
	public byte[] read() throws DiskReadError;
	public void write(byte data[]) throws DiskWriteError;
}
