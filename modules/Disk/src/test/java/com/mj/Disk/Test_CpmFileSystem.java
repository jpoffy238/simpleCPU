package com.mj.Disk;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.mj.Disk.EightInch.IBM3740;
import com.mj.Disk.exceptions.FileSystemException;

/** CpmFileSystem operates directly on a FloppyDevice - no FloppyController/VIA/bus
 * needed, since this is purely filesystem logic layered on raw sector read/write. */
public class Test_CpmFileSystem {
	@TempDir
	Path tempDir;

	private IBM3740 drive;
	private CpmFileSystem fs;

	@BeforeEach
	public void setup() throws Exception {
		drive = new IBM3740();
		drive.setVirtualDiskFile(tempDir.resolve("drive0.img").toString());
		fs = new CpmFileSystem(drive);
		fs.format();
	}

	@Test
	public void formatLeavesTheDirectoryEmpty() throws Exception {
		assertTrue(fs.listFiles().isEmpty());
	}

	@Test
	public void createThenReadFileRoundTripsWithinASingleBlock() throws Exception {
		byte[] data = "Hello, CP/M!".getBytes();
		fs.createFile("HELLO.TXT", data);

		byte[] readBack = fs.readFile("HELLO.TXT");

		// Length is rounded up to the nearest 128-byte record, like real CP/M.
		assertEquals(128, readBack.length);
		assertArrayEquals(data, Arrays.copyOf(readBack, data.length));
	}

	@Test
	public void createThenReadFileRoundTripsAcrossATrackBoundary() throws Exception {
		// Blocks 0-1 are the directory, so the first file created gets block 2
		// onward. A ~3000-byte file needs blocks 2, 3 and 4 - block 3 is the one
		// that actually spans the track 1/track 2 boundary (26 sectors/track isn't
		// divisible by 8 sectors/block), so this exercises the trickiest part of
		// the block<->(track,sector) translation.
		byte[] data = new byte[3000];
		for (int i = 0; i < data.length; i++) {
			data[i] = (byte) i;
		}
		fs.createFile("PATTERN.BIN", data);

		byte[] readBack = fs.readFile("PATTERN.BIN");

		assertEquals(3072, readBack.length, "rounded up to the nearest 128-byte record");
		assertArrayEquals(data, Arrays.copyOf(readBack, data.length));
	}

	@Test
	public void listFilesReturnsEveryCreatedFile() throws Exception {
		fs.createFile("ONE.TXT", "one".getBytes());
		fs.createFile("TWO.TXT", "two".getBytes());

		List<String> names = fs.listFiles();

		assertEquals(2, names.size());
		assertTrue(names.contains("ONE.TXT"));
		assertTrue(names.contains("TWO.TXT"));
	}

	@Test
	public void secondFileDoesNotOverwriteTheFirst() throws Exception {
		byte[] first = "first file contents".getBytes();
		byte[] second = "second, different file contents".getBytes();
		fs.createFile("FIRST.TXT", first);
		fs.createFile("SECOND.TXT", second);

		assertArrayEquals(first, Arrays.copyOf(fs.readFile("FIRST.TXT"), first.length));
		assertArrayEquals(second, Arrays.copyOf(fs.readFile("SECOND.TXT"), second.length));
	}

	@Test
	public void creatingADuplicateNameThrows() throws Exception {
		fs.createFile("DUP.TXT", "first".getBytes());

		assertThrows(FileSystemException.class, () -> fs.createFile("DUP.TXT", "second".getBytes()));
	}

	@Test
	public void readingAMissingFileThrows() {
		assertThrows(FileSystemException.class, () -> fs.readFile("NOPE.TXT"));
	}

	@Test
	public void deletedFileNoLongerAppearsOrReads() throws Exception {
		fs.createFile("GONE.TXT", "temporary".getBytes());

		fs.deleteFile("GONE.TXT");

		assertTrue(fs.listFiles().isEmpty());
		assertThrows(FileSystemException.class, () -> fs.readFile("GONE.TXT"));
	}

	@Test
	public void deletingAFileFreesItsBlocksForReuse() throws Exception {
		byte[] data = new byte[3000]; // spans blocks 2-4, same as the track-boundary test
		fs.createFile("BIG.BIN", data);
		fs.deleteFile("BIG.BIN");

		// Re-creating a same-size file should succeed by reusing the freed blocks,
		// not fail as if the disk were still full of BIG.BIN's old allocation.
		fs.createFile("AGAIN.BIN", data);

		assertArrayEquals(data, Arrays.copyOf(fs.readFile("AGAIN.BIN"), data.length));
	}

	@Test
	public void deletingAMissingFileThrows() {
		assertThrows(FileSystemException.class, () -> fs.deleteFile("NOPE.TXT"));
	}

	@Test
	public void deletingAndRecreatingTheSameNameWorks() throws Exception {
		fs.createFile("DUP.TXT", "first".getBytes());
		fs.deleteFile("DUP.TXT");

		fs.createFile("DUP.TXT", "second".getBytes());

		assertArrayEquals("second".getBytes(), Arrays.copyOf(fs.readFile("DUP.TXT"), 6));
	}

	@Test
	public void fileSpanningTwoExtentsRoundTrips() throws Exception {
		// One extent holds MAX_BLOCKS_PER_FILE * 1024 = 16384 bytes - this needs a
		// second extent (and so a second directory entry sharing BIG.BIN's name,
		// distinguished by EX) to hold the rest.
		byte[] data = new byte[20000];
		for (int i = 0; i < data.length; i++) {
			data[i] = (byte) i;
		}
		fs.createFile("BIG.BIN", data);

		byte[] readBack = fs.readFile("BIG.BIN");

		assertEquals(20096, readBack.length, "rounded up to the nearest 128-byte record");
		assertArrayEquals(data, Arrays.copyOf(readBack, data.length));
	}

	@Test
	public void fileSpanningThreeExtentsRoundTrips() throws Exception {
		// Three full-ish extents - makes sure the "every extent but the last is
		// full" chaining logic isn't accidentally special-cased to exactly two.
		byte[] data = new byte[40000];
		for (int i = 0; i < data.length; i++) {
			data[i] = (byte) (i * 7);
		}
		fs.createFile("THREE.BIN", data);

		byte[] readBack = fs.readFile("THREE.BIN");

		assertArrayEquals(data, Arrays.copyOf(readBack, data.length));
	}

	@Test
	public void multiExtentFileAppearsOnceInListFiles() throws Exception {
		fs.createFile("BIG.BIN", new byte[20000]);
		fs.createFile("SMALL.TXT", "tiny".getBytes());

		List<String> names = fs.listFiles();

		assertEquals(2, names.size(), "BIG.BIN's second extent shouldn't add a duplicate entry");
		assertTrue(names.contains("BIG.BIN"));
		assertTrue(names.contains("SMALL.TXT"));
	}

	@Test
	public void deletingAMultiExtentFileFreesAllItsExtentsForReuse() throws Exception {
		byte[] data = new byte[20000];
		fs.createFile("BIG.BIN", data);

		fs.deleteFile("BIG.BIN");

		assertTrue(fs.listFiles().isEmpty());
		assertThrows(FileSystemException.class, () -> fs.readFile("BIG.BIN"));
		// Re-creating a same-size file should succeed by reusing every block freed
		// across both extents, not just the first one.
		fs.createFile("AGAIN.BIN", data);
		assertArrayEquals(data, Arrays.copyOf(fs.readFile("AGAIN.BIN"), data.length));
	}

	@Test
	public void fileNameLongerThanEightDotThreeThrows() {
		assertThrows(FileSystemException.class, () -> fs.createFile("WAYTOOLONG.TXT", "x".getBytes()));
		assertThrows(FileSystemException.class, () -> fs.createFile("OK.TOOLONG", "x".getBytes()));
	}
}
