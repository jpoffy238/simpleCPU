package com.mj.Disk.EightInch;

import com.mj.Disk.DiskGeometry;
import com.mj.Disk.FloppyDevice;
import com.mj.Disk.exceptions.DiskReadError;
import com.mj.Disk.exceptions.DiskWriteError;
import com.mj.Disk.exceptions.GeometryException;

public abstract class IBM3740 implements FloppyDevice {
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
	
	private DiskGeometry geometry;
	private byte[] buffer;
	
	public IBM3740() {
		geometry = new DiskGeometry();
		geometry.setHeads(1);
		geometry.setSectors(26);
		geometry.setTracks(76);
		geometry.setSectorSize(128);
		buffer = new byte[128];
	}
	
	@Override
	public DiskGeometry getGeometry() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setVirutalDiskFile(String fname) {
		// TODO Auto-generated method stub

	}

	@Override
	public void format() throws GeometryException, DiskWriteError {
		// TODO Auto-generated method stub

	}

	@Override
	public void setHead(int headId) throws GeometryException {
		// TODO Auto-generated method stub

	}

	@Override
	public void setTrack(int trackId) throws GeometryException {
		// TODO Auto-generated method stub

	}

	@Override
	public void setSector(int sectorId) throws GeometryException {
		// TODO Auto-generated method stub

	}

	@Override
	public byte[] read() throws DiskReadError {
		// TODO Auto-generated method stub
		return buffer;
	}

	@Override
	public void write(byte[] data) throws DiskWriteError {
		buffer = data.clone();
		

	}

}
