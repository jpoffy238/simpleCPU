package com.mj.Disk;

import com.mj.Disk.DiskGeometry;
public interface VitrtualDisk {
	public VitrtualDisk createDisk(DiskGeometry geometry, String fileName);
	public void seek(int head, int track);
	public byte[] read(int sector);
	public void write (byte [] data);
	public void sync();
	public byte status();
	public byte cmd(byte cmd);
	// cmds include drive motor on/off
	//	head load/unload
	// 
	
}
