package com.mj.Disk.Imd;

/** The handful of real CP/M "Disk Parameter Block" fields CpmImageReader actually
 * needs to interpret a directory/file layout that isn't this project's own fixed
 * CpmFileSystem convention - see that class's own comment for why this is a
 * separate reader rather than a generalization of it. Expressed directly against
 * ImdImage's own flat, already-deblocked 128-byte-record byte stream (see
 * ImdImage.toFlatBytes()), not in terms of tracks/physical sector sizes the way a
 * real BIOS's own DPB would - that complexity is already resolved by the time this
 * class's input exists. */
public final class DiskParameterBlock {

	public static final int RECORD_SIZE = 128;
	public static final int ENTRY_SIZE = 32;
	public static final int ENTRIES_PER_RECORD = RECORD_SIZE / ENTRY_SIZE;
	/** Real CP/M's own fixed per-entry layout: 16 bytes (offsets 16-31) hold block
	 * pointers, regardless of how wide each one is. */
	private static final int POINTER_REGION_BYTES = 16;

	private final int reservedRecords;
	private final int recordsPerBlock;
	private final int directoryBlocks;
	private final int blockPointerWidth;

	/**
	 * @param reservedRecords   128-byte records before the directory/data area
	 *                          begins (boot tracks, deblocked - see the class
	 *                          comment).
	 * @param recordsPerBlock   128-byte records per allocation block (BLS / 128).
	 * @param directoryBlocks   allocation blocks reserved for the directory; block
	 *                          numbers 0..directoryBlocks-1 are the directory, data
	 *                          blocks start at directoryBlocks.
	 * @param blockPointerWidth bytes per block pointer in a directory entry (1 or 2 -
	 *                          2 once a disk has more than 255 blocks).
	 */
	public DiskParameterBlock(int reservedRecords, int recordsPerBlock, int directoryBlocks, int blockPointerWidth) {
		if (blockPointerWidth != 1 && blockPointerWidth != 2) {
			throw new IllegalArgumentException("blockPointerWidth must be 1 or 2, was " + blockPointerWidth);
		}
		// Real CP/M's RC byte (0-128) is only a valid "this extent is full" sentinel
		// when a full extent's block pointers cover exactly 128 records - true for
		// every DPB this project has actually seen (this project's own fixed format,
		// and the one real external disk validated so far), but not a universal
		// guarantee (a different EXM/BLS combination can need genuinely more complex
		// logical-vs-physical extent folding). Reject anything else loudly rather
		// than silently mis-reading a file at its first extent boundary.
		int recordsPerExtent = recordsPerBlock * (POINTER_REGION_BYTES / blockPointerWidth);
		if (recordsPerExtent != 128) {
			throw new IllegalArgumentException(
					"recordsPerBlock (" + recordsPerBlock + ") * pointers-per-extent (" + (POINTER_REGION_BYTES / blockPointerWidth)
							+ ") must be 128, was " + recordsPerExtent + " - this DPB needs real EXM handling CpmImageReader doesn't implement");
		}
		this.reservedRecords = reservedRecords;
		this.recordsPerBlock = recordsPerBlock;
		this.directoryBlocks = directoryBlocks;
		this.blockPointerWidth = blockPointerWidth;
	}

	public int reservedRecords() {
		return reservedRecords;
	}

	public int recordsPerBlock() {
		return recordsPerBlock;
	}

	public int directoryBlocks() {
		return directoryBlocks;
	}

	public int blockPointerWidth() {
		return blockPointerWidth;
	}

	public int directoryEntryCount() {
		return directoryBlocks * recordsPerBlock * ENTRIES_PER_RECORD;
	}

	/** How many block pointers fit in one directory entry's fixed 16-byte pointer
	 * region at this pointer width - fewer of them fit as they get wider. */
	public int pointersPerExtent() {
		return POINTER_REGION_BYTES / blockPointerWidth;
	}

	/** The real TRS-80 Model II / CP/M 2.2 8" disk this project validated against
	 * (see CpmImageReader's class comment): a single-density 128-byte-sector boot
	 * track plus a 512-byte-sector system track (26 + 64 = 90 records once
	 * deblocked), BLS=2048 (16 records/block), one directory block, 16-bit block
	 * pointers (needed past 255 blocks). */
	public static DiskParameterBlock trs80ModelIiCpm22() {
		return new DiskParameterBlock(90, 16, 1, 2);
	}
}
