package com.mj.Disk;

public interface VitrtualDisk {
	public object createDisk(DiskGeometry geometry, String fileName);
	public seekTo(int head, int track, int sector);
	public byte[] read();
	public void write (byte [] data);
	
}
