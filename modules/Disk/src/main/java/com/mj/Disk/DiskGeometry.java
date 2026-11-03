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

	public boolean isValidHead(int head) {
		return head >= 0 && head < heads;
	}

	public boolean isValidTrack(int track) {
		return track >= 0 && track < tracks;
	}

	/** Sectors are numbered 1..sectors, matching the IBM 3740's Rec# field. */
	public boolean isValidSector(int sector) {
		return sector >= 1 && sector <= sectors;
	}

	public long totalBytes() {
		return (long) heads * tracks * sectors * sectorSize;
	}

	/** Byte offset of a sector's start within a flat, track-ordered disk image. */
	public long offset(int head, int track, int sector) {
		long tracksBefore = (long) head * tracks + track;
		long sectorsBefore = tracksBefore * sectors + (sector - 1);
		return sectorsBefore * sectorSize;
	}
}
