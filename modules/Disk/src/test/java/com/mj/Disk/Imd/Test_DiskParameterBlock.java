package com.mj.Disk.Imd;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

public class Test_DiskParameterBlock {

	@Test
	public void rejectsABlockPointerWidthOtherThanOneOrTwo() {
		assertThrows(IllegalArgumentException.class, () -> new DiskParameterBlock(0, 8, 2, 3));
	}

	@Test
	public void rejectsARecordsPerExtentOtherThan128() {
		// recordsPerBlock=1 * pointersPerExtent=16 (width 1) = 16, not 128.
		assertThrows(IllegalArgumentException.class, () -> new DiskParameterBlock(0, 1, 2, 1));
	}

	@Test
	public void acceptsThisProjectsOwnFixedFormatShape() {
		// SECTORS_PER_BLOCK=8 (128-byte records), 8-bit pointers: 8 * 16 = 128.
		DiskParameterBlock dpb = new DiskParameterBlock(0, 8, 2, 1);

		assertEquals(16, dpb.pointersPerExtent());
		assertEquals(64, dpb.directoryEntryCount()); // 2 blocks * 8 records * 4 entries/record
	}

	@Test
	public void acceptsTheRealTrs80Model2Cpm22Shape() {
		DiskParameterBlock dpb = DiskParameterBlock.trs80ModelIiCpm22();

		assertEquals(90, dpb.reservedRecords());
		assertEquals(16, dpb.recordsPerBlock());
		assertEquals(1, dpb.directoryBlocks());
		assertEquals(2, dpb.blockPointerWidth());
		assertEquals(8, dpb.pointersPerExtent()); // 16 / 2
		assertEquals(64, dpb.directoryEntryCount()); // 1 block * 16 records * 4 entries/record
	}
}
