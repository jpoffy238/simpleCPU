package com.mj.Disk.EightInch;

import java.io.File;

import com.mj.Disk.DiskGeometry;
import com.mj.Disk.FileVirtualDisk;
import com.mj.Disk.FloppyDevice;
import com.mj.Disk.VirtualDisk;
import com.mj.Disk.exceptions.DiskReadError;
import com.mj.Disk.exceptions.DiskWriteError;
import com.mj.Disk.exceptions.GeometryException;

/*
 * SSSD 26 Each 128 Byte Sector Track Format
This is an IBM 3740 Standard format.

Field	Length	Data	Clock	Value
Gap I	  40	0xFF	NA	0xFF
Sync I	   6	0x00	NA	0x00
IM	1	0xFC	0xD7	0xFC
GAP III	  26	0xFF	NA	0xFF
Following repeated 26 times to ***
Sync III   6	0x00	NA	0x00
AM 1	   1	0xFE	0xC7	0xFE
Cyl #	   1	0-76	NA	0-76
Head #	   1	0	NA	0
Rec #	   1	1-26	NA	1-26
N	       1	0	NA	0
CRC	       2	CRC	NA	0xF7 (generates both CRC bytes)
GAP II	  11	0xFF	NA	0xFF
Sync II	   6	0x00	NA	0x00
AM 2	   1	0xFB	0xC7	0xFB
Data	 128	0xE5	NA	0xE5
CRC	       2     CRC	NA	0xF7 (generates both CRC bytes)
GAP III	  27	0xFF	NA	0xFF
***
GAP IV	~221	0xFF	NA	0xFF
Total Bytes/Sector	189
Total Bytes/Track	5208
 */

// Only the 128-byte sector payload is emulated; gap/sync/AM/CRC bytes above are
// the real IBM 3740 wire format and are not simulated.
public class IBM3740 implements FloppyDevice {

	private DiskGeometry geometry;
	private VirtualDisk disk = new FileVirtualDisk();
	private String fileName;
	private int head;
	private int track;
	private int sector = 1;

	public IBM3740() {
		geometry = new DiskGeometry();
		geometry.setHeads(1);
		geometry.setSectors(26);
		geometry.setTracks(76);
		geometry.setSectorSize(128);
	}

	@Override
	public DiskGeometry getGeometry() {
		return geometry;
	}

	@Override
	public void setVirtualDiskFile(String fname) throws DiskReadError, DiskWriteError {
		fileName = fname;
		if (new File(fname).exists()) {
			disk.open(geometry, fname);
		} else {
			disk.create(geometry, fname);
		}
	}

	@Override
	public void format() throws GeometryException, DiskWriteError {
		disk.create(geometry, fileName);
	}

	@Override
	public void setHead(int headId) throws GeometryException {
		if (!geometry.isValidHead(headId)) {
			throw new GeometryException("Invalid head " + headId);
		}
		head = headId;
	}

	@Override
	public void setTrack(int trackId) throws GeometryException {
		if (!geometry.isValidTrack(trackId)) {
			throw new GeometryException("Invalid track " + trackId);
		}
		track = trackId;
	}

	@Override
	public void setSector(int sectorId) throws GeometryException {
		if (!geometry.isValidSector(sectorId)) {
			throw new GeometryException("Invalid sector " + sectorId);
		}
		sector = sectorId;
	}

	@Override
	public void spinUp() {
	}

	@Override
	public void spinDown() {
	}

	@Override
	public void loadHead() {
	}

	@Override
	public void unloadHead() {
	}

	@Override
	public byte[] read() throws DiskReadError {
		try {
			return disk.read(head, track, sector);
		} catch (GeometryException e) {
			throw new DiskReadError(e.getMessage(), e);
		}
	}

	@Override
	public void write(byte[] data) throws DiskWriteError {
		try {
			disk.write(head, track, sector, data);
		} catch (GeometryException e) {
			throw new DiskWriteError(e.getMessage(), e);
		}
	}

}
