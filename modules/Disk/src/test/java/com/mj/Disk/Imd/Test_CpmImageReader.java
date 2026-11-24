package com.mj.Disk.Imd;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.mj.Disk.exceptions.FileSystemException;

/** Exercises CpmImageReader against synthetic flat images built by hand, rather
 * than a real .IMD fixture - see Test_ImdImage for the format-decoding half, and
 * the manual verification against the real TRS-80 Model II CP/M 2.2 disk image
 * noted in CpmImageReader's own class comment for why this class's understanding
 * of the format is trusted beyond just these synthetic cases. */
public class Test_CpmImageReader {

	/** Builds a flat, already-deblocked CP/M image byte array - the same shape
	 * CpmFileSystem.createFile() produces, generalized over an arbitrary
	 * DiskParameterBlock, so each test can just say "this file has this content"
	 * rather than hand-computing byte offsets. Blocks 0..directoryBlocks-1 are the
	 * directory; data blocks are handed out sequentially starting right after it -
	 * real CP/M allocation order, though real CP/M doesn't guarantee contiguous
	 * allocation the way this always does. */
	private static byte[] buildImage(DiskParameterBlock dpb, Map<String, byte[]> files) throws Exception {
		int blockBytes = dpb.recordsPerBlock() * DiskParameterBlock.RECORD_SIZE;
		int recordsPerExtent = dpb.pointersPerExtent() * dpb.recordsPerBlock(); // always 128 - enforced by the DPB itself

		byte[][] directoryEntries = new byte[dpb.directoryEntryCount()][];
		for (int i = 0; i < directoryEntries.length; i++) {
			directoryEntries[i] = new byte[DiskParameterBlock.ENTRY_SIZE];
			Arrays.fill(directoryEntries[i], (byte) 0xE5);
		}

		List<byte[]> dataBlocks = new ArrayList<byte[]>();
		int nextBlock = dpb.directoryBlocks();
		int nextDirEntry = 0;

		for (Map.Entry<String, byte[]> file : files.entrySet()) {
			byte[] fcbName = CpmImageReader.encodeFileName(file.getKey());
			byte[] content = file.getValue();
			int totalRecords = (content.length + 127) / 128;
			int extentsNeeded = Math.max(1, (totalRecords + recordsPerExtent - 1) / recordsPerExtent);
			int copied = 0;

			for (int extent = 0; extent < extentsNeeded; extent++) {
				int recordsThisExtent = Math.min(recordsPerExtent, totalRecords - extent * recordsPerExtent);
				int blocksThisExtent = (recordsThisExtent + dpb.recordsPerBlock() - 1) / dpb.recordsPerBlock();

				byte[] entry = new byte[DiskParameterBlock.ENTRY_SIZE];
				System.arraycopy(fcbName, 0, entry, 1, 11);
				entry[12] = (byte) extent;
				entry[15] = (byte) recordsThisExtent;

				for (int i = 0; i < blocksThisExtent; i++) {
					byte[] blockData = new byte[blockBytes];
					int remaining = Math.max(0, Math.min(blockBytes, content.length - copied));
					if (remaining > 0) {
						System.arraycopy(content, copied, blockData, 0, remaining);
					}
					copied += blockBytes;
					int blockNumber = nextBlock++;
					dataBlocks.add(blockData);
					writePointer(entry, i, blockNumber, dpb.blockPointerWidth());
				}

				directoryEntries[nextDirEntry++] = entry;
			}
		}

		int reservedBytes = dpb.reservedRecords() * DiskParameterBlock.RECORD_SIZE;
		int directoryBytes = dpb.directoryBlocks() * blockBytes;
		byte[] result = new byte[reservedBytes + directoryBytes + dataBlocks.size() * blockBytes];
		int offset = reservedBytes;
		for (byte[] entry : directoryEntries) {
			System.arraycopy(entry, 0, result, offset, DiskParameterBlock.ENTRY_SIZE);
			offset += DiskParameterBlock.ENTRY_SIZE;
		}
		for (byte[] block : dataBlocks) {
			System.arraycopy(block, 0, result, offset, blockBytes);
			offset += blockBytes;
		}
		return result;
	}

	private static void writePointer(byte[] entry, int index, int blockNumber, int width) {
		int base = 16 + index * width;
		entry[base] = (byte) (blockNumber & 0xff);
		if (width == 2) {
			entry[base + 1] = (byte) ((blockNumber >> 8) & 0xff);
		}
	}

	@Test
	public void listsOnlyNonEmptyFirstExtentEntries() throws Exception {
		DiskParameterBlock dpb = new DiskParameterBlock(0, 8, 2, 1);
		byte[] image = buildImage(dpb, Map.of("HELLO.TXT", "Hello, CP/M!".getBytes()));

		CpmImageReader reader = new CpmImageReader(image, dpb);

		assertEquals(List.of("HELLO.TXT"), reader.listFiles());
	}

	@Test
	public void readsBackASingleExtentFileWithEightBitPointers() throws Exception {
		DiskParameterBlock dpb = new DiskParameterBlock(0, 8, 2, 1);
		byte[] content = "Hello, CP/M!".getBytes();
		byte[] image = buildImage(dpb, Map.of("HELLO.TXT", content));

		byte[] readBack = new CpmImageReader(image, dpb).readFile("HELLO.TXT");

		assertEquals(128, readBack.length); // rounded up to the nearest record
		assertArrayEquals(content, Arrays.copyOf(readBack, content.length));
	}

	@Test
	public void readsBackAFileWithSixteenBitBlockPointers() throws Exception {
		// Same shape as the real TRS-80 Model II disk: 16 records/block, 16-bit
		// pointers (8 of them fit in the fixed 16-byte pointer region).
		DiskParameterBlock dpb = DiskParameterBlock.trs80ModelIiCpm22();
		byte[] content = "A file on a disk with 16-bit block pointers.".getBytes();
		byte[] image = buildImage(dpb, Map.of("WIDE.TXT", content));

		byte[] readBack = new CpmImageReader(image, dpb).readFile("WIDE.TXT");

		assertArrayEquals(content, Arrays.copyOf(readBack, content.length));
	}

	@Test
	public void readingAMissingFileThrows() throws Exception {
		DiskParameterBlock dpb = new DiskParameterBlock(0, 8, 2, 1);
		byte[] image = buildImage(dpb, Map.of());

		CpmImageReader reader = new CpmImageReader(image, dpb);

		assertThrows(FileSystemException.class, () -> reader.readFile("NOPE.TXT"));
	}

	@Test
	public void listFilesAndReadFileIgnoreTheAttributeFlagHighBitOnNameBytes() throws Exception {
		// Real CP/M (2.2 and Plus alike) uses bit 7 of each name/extension byte
		// as a per-character file attribute flag (read-only, system, archive) -
		// found on a real CompuPro CP/M Plus disk (see
		// DiskParameterBlock.compuProCpmPlus()), which sets it on several
		// files; a synthetic image built by encodeFileName() alone never sets
		// it, so this flips it by hand on two bytes to prove both
		// listFiles()/readFile() tolerate it rather than only working by
		// accident on a disk that happens to never set it.
		DiskParameterBlock dpb = new DiskParameterBlock(0, 8, 2, 1);
		byte[] content = "Hello, CP/M!".getBytes();
		byte[] image = buildImage(dpb, Map.of("HELLO.TXT", content));
		image[1] |= 0x80; // 'H' of HELLO
		image[9] |= 0x80; // 'T' of the TXT extension

		CpmImageReader reader = new CpmImageReader(image, dpb);

		assertEquals(List.of("HELLO.TXT"), reader.listFiles());
		assertArrayEquals(content, Arrays.copyOf(reader.readFile("HELLO.TXT"), content.length));
	}

	@Test
	public void listFilesSkipsCpmPlusSpecialPurposeDirectoryEntries() throws Exception {
		// CP/M Plus's date-stamping extension reserves periodic directory
		// slots for timestamp/password metadata (status byte 0x21 on a real
		// CompuPro CP/M Plus disk - see CpmImageReader's own class comment),
		// not an ordinary file - a status byte this isn't STATUS_EMPTY (0xE5)
		// but also isn't a real 0-15 user number. Writes one directly into an
		// otherwise-empty directory slot, at the same extent (0) a real file
		// also uses, to prove this is excluded on its own status rather than
		// by some incidental extent mismatch.
		DiskParameterBlock dpb = new DiskParameterBlock(0, 8, 2, 1);
		byte[] image = buildImage(dpb, Map.of("HELLO.TXT", "Hello, CP/M!".getBytes()));
		int specialOffset = DiskParameterBlock.ENTRY_SIZE; // slot 1 - otherwise still 0xE5
		image[specialOffset] = 0x21;
		for (int i = 1; i < 12; i++) {
			image[specialOffset + i] = 'X';
		}
		image[specialOffset + 12] = 0; // extent 0, same as the real file above

		CpmImageReader reader = new CpmImageReader(image, dpb);

		assertEquals(List.of("HELLO.TXT"), reader.listFiles());
	}

	@Test
	public void readsBackAFileSpanningMultipleExtents() throws Exception {
		DiskParameterBlock dpb = new DiskParameterBlock(0, 8, 2, 1); // 16 pointers * 8 records = 128 records/extent
		byte[] content = new byte[17664]; // 138 records - crosses the extent 0/1 boundary at 128 records
		for (int i = 0; i < content.length; i++) {
			content[i] = (byte) i;
		}
		byte[] image = buildImage(dpb, Map.of("BIG.TXT", content));

		byte[] readBack = new CpmImageReader(image, dpb).readFile("BIG.TXT");

		assertEquals(17664, readBack.length); // already an exact multiple of 128
		assertArrayEquals(content, readBack);
	}
}
