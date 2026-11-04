package com.mj.Disk;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.mj.Disk.exceptions.DiskReadError;

/** FileVirtualDisk's 16-byte self-describing header: written on create(), validated
 * (not discovered) against the caller's own geometry on open(). */
public class Test_FileVirtualDisk {
	@TempDir
	Path tempDir;

	private DiskGeometry geometry(int heads, int tracks, int sectors, int sectorSize) {
		DiskGeometry g = new DiskGeometry();
		g.setHeads(heads);
		g.setTracks(tracks);
		g.setSectors(sectors);
		g.setSectorSize(sectorSize);
		return g;
	}

	@Test
	public void createThenOpenWithTheSameGeometryRoundTrips() throws Exception {
		String fileName = tempDir.resolve("drive.img").toString();
		DiskGeometry geometry = geometry(1, 76, 26, 128);

		FileVirtualDisk disk = new FileVirtualDisk();
		disk.create(geometry, fileName);
		byte[] written = new byte[128];
		for (int i = 0; i < written.length; i++) {
			written[i] = (byte) i;
		}
		disk.write(0, 5, 3, written);

		FileVirtualDisk reopened = new FileVirtualDisk();
		reopened.open(geometry, fileName);
		assertArrayEquals(written, reopened.read(0, 5, 3));
	}

	@Test
	public void openRejectsAFileWhoseStoredGeometryDoesNotMatch() throws Exception {
		String fileName = tempDir.resolve("drive.img").toString();
		new FileVirtualDisk().create(geometry(1, 76, 26, 128), fileName);

		FileVirtualDisk mismatched = new FileVirtualDisk();
		assertThrows(DiskReadError.class,
				() -> mismatched.open(geometry(2, 77, 26, 128), fileName),
				"a drive expecting different geometry shouldn't silently accept this image");
	}

	@Test
	public void openRejectsAFileWithoutTheMagicHeader() throws Exception {
		String fileName = tempDir.resolve("notadisk.img").toString();
		java.nio.file.Files.write(java.nio.file.Paths.get(fileName), new byte[32]);

		FileVirtualDisk disk = new FileVirtualDisk();
		assertThrows(DiskReadError.class, () -> disk.open(geometry(1, 76, 26, 128), fileName));
	}

	@Test
	public void recreatingAnExistingImageBlanksItsBody() throws Exception {
		String fileName = tempDir.resolve("drive.img").toString();
		DiskGeometry geometry = geometry(1, 76, 26, 128);

		FileVirtualDisk disk = new FileVirtualDisk();
		disk.create(geometry, fileName);
		byte[] pattern = new byte[128];
		for (int i = 0; i < pattern.length; i++) {
			pattern[i] = (byte) 0xFF;
		}
		disk.write(0, 0, 1, pattern);

		disk.create(geometry, fileName);
		byte[] afterReformat = disk.read(0, 0, 1);
		for (byte b : afterReformat) {
			assertEquals(0, b, "reformatting should blank old data, not just resize");
		}
	}
}
