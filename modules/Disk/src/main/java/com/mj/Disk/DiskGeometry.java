package com.mj.Disk;

public class DiskGeometry {
	public int getHeads() {
		return heads;
	}
	public void setHeads(int heads) {
		this.heads = heads;
	}
	public int getTracks() {
		return tracks;
	}
	public void setTracks(int tracks) {
		this.tracks = tracks;
	}
	public int getSectors() {
		return sectors;
	}
	public void setSectors(int sectors) {
		this.sectors = sectors;
	}
	public int getSectorSize() {
		return sectorSize;
	}
	public void setSectorSize(int sectorSize) {
		this.sectorSize = sectorSize;
	}
	private int heads;
	private int tracks;
	private int sectors;
	private int sectorSize;
	
	
	
}
