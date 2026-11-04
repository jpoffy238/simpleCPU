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
	public void fileLargerThanOneExtentThrows() {
		byte[] tooBig = new byte[CpmFileSystem.MAX_BLOCKS_PER_FILE * 1024 + 1];

		assertThrows(FileSystemException.class, () -> fs.createFile("BIG.BIN", tooBig));
	}

	@Test
	public void fileNameLongerThanEightDotThreeThrows() {
		assertThrows(FileSystemException.class, () -> fs.createFile("WAYTOOLONG.TXT", "x".getBytes()));
		assertThrows(FileSystemException.class, () -> fs.createFile("OK.TOOLONG", "x".getBytes()));
	}
}
