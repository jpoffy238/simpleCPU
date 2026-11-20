package com.mj.Disk.Imd;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.mj.Disk.exceptions.ImdFormatException;

/** Builds small synthetic .IMD byte streams by hand rather than depending on a real
 * fixture file - the format is simple enough to construct directly, and this keeps
 * the suite deterministic/self-contained (a real disk image, when one is used, is a
 * separate integration test gated on the file actually being present - see
 * CpmFileSystem-adjacent tests for that pattern once a CP/M-specific reader exists). */
public class Test_ImdImage {

	private static byte[] image(String comment, byte[]... trackRecords) {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		out.writeBytes(comment.getBytes(StandardCharsets.US_ASCII));
		out.write(0x1A);
		for (byte[] record : trackRecords) {
			out.writeBytes(record);
		}
		return out.toByteArray();
	}

	/** mode=0 (irrelevant to decoding), no cylinder/head map, one data-record type
	 * uniformly applied to every sector - covers normal (1), compressed (2) and
	 * unavailable (0) via payloads/fillByte as appropriate for that type. */
	private static byte[] track(int cylinder, int head, int sizeCode, int recordType, int[] sectorNumbers,
			byte[][] normalPayloads, Byte fillByte) {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		out.write(0); // mode
		out.write(cylinder);
		out.write(head);
		out.write(sectorNumbers.length);
		out.write(sizeCode);
		for (int n : sectorNumbers) {
			out.write(n);
		}
		for (int i = 0; i < sectorNumbers.length; i++) {
			out.write(recordType);
			if (recordType == 0) {
				// unavailable - no data follows
			} else if (recordType == 2) {
				out.write(fillByte);
			} else {
				out.writeBytes(normalPayloads[i]);
			}
		}
		return out.toByteArray();
	}

	@Test
	public void parsesTheCommentAndStopsAtTheTerminatorByte() throws Exception {
		byte[] data = image("IMD 1.16: a real comment\r\n\r\nEight Inch SSSD.\r\n\r\n",
				track(0, 0, 0, 1, new int[] { 1 }, new byte[][] { new byte[128] }, null));

		ImdImage image = ImdImage.parse(data);

		assertTrue(image.comment().contains("Eight Inch SSSD."));
	}

	@Test
	public void decodesNormalSectorDataAndGeometry() throws Exception {
		byte[] sector1 = new byte[128];
		sector1[0] = 'A';
		byte[] sector2 = new byte[128];
		sector2[0] = 'B';
		byte[] data = image("comment",
				track(5, 0, 0, 1, new int[] { 1, 2 }, new byte[][] { sector1, sector2 }, null));

		ImdImage image = ImdImage.parse(data);
		List<ImdImage.Track> tracks = image.tracks();

		assertEquals(1, tracks.size());
		ImdImage.Track track = tracks.get(0);
		assertEquals(5, track.cylinder());
		assertEquals(0, track.head());
		assertEquals(128, track.sectorSize());
		assertEquals(java.util.Set.of(1, 2), track.sectorNumbers());
		assertArrayEquals(sector1, track.sector(1));
		assertArrayEquals(sector2, track.sector(2));
	}

	@Test
	public void decodesACompressedSectorAsARepeatedFillByte() throws Exception {
		byte[] data = image("comment",
				track(0, 0, 0, 2, new int[] { 1 }, null, (byte) 0xE5));

		ImdImage image = ImdImage.parse(data);

		byte[] expected = new byte[128];
		java.util.Arrays.fill(expected, (byte) 0xE5);
		assertArrayEquals(expected, image.tracks().get(0).sector(1));
	}

	@Test
	public void anUnavailableSectorReadsAsNullButFlattensAsZeros() throws Exception {
		byte[] data = image("comment",
				track(0, 0, 0, 0, new int[] { 1 }, null, null));

		ImdImage image = ImdImage.parse(data);

		assertNull(image.tracks().get(0).sector(1));
		assertArrayEquals(new byte[128], image.toFlatBytes());
	}

	@Test
	public void tracksByPositionSortsRegardlessOfFileOrder() throws Exception {
		byte[] sector0 = new byte[128];
		sector0[0] = 'X';
		byte[] sector1 = new byte[128];
		sector1[0] = 'Y';
		// Deliberately stored out of order in the file - cylinder 1 before cylinder 0.
		byte[] data = image("comment",
				track(1, 0, 0, 1, new int[] { 1 }, new byte[][] { sector1 }, null),
				track(0, 0, 0, 1, new int[] { 1 }, new byte[][] { sector0 }, null));

		ImdImage image = ImdImage.parse(data);

		assertEquals(1, image.tracks().get(0).cylinder(), "tracks() keeps file order");
		List<ImdImage.Track> byPosition = image.tracksByPosition();
		assertEquals(0, byPosition.get(0).cylinder());
		assertEquals(1, byPosition.get(1).cylinder());
	}

	@Test
	public void toFlatBytesConcatenatesTracksInPositionOrderWithMixedSectorSizes() throws Exception {
		byte[] bootSector = new byte[128];
		bootSector[0] = 'B';
		byte[] dataSector = new byte[512];
		dataSector[0] = 'D';
		byte[] data = image("comment",
				track(0, 0, 0, 1, new int[] { 1 }, new byte[][] { bootSector }, null), // 128-byte boot track
				track(1, 0, 2, 1, new int[] { 1 }, new byte[][] { dataSector }, null)); // 512-byte data track

		byte[] flat = ImdImage.parse(data).toFlatBytes();

		assertEquals(128 + 512, flat.length);
		assertEquals('B', flat[0]);
		assertEquals('D', flat[128]);
	}

	@Test
	public void rejectsAFileWithNoCommentTerminator() {
		byte[] data = "no terminator byte here".getBytes(StandardCharsets.US_ASCII);

		assertThrows(ImdFormatException.class, () -> ImdImage.parse(data));
	}

	@Test
	public void rejectsAnUnsupportedVariableSectorSizeCode() {
		byte[] data = image("comment", track(0, 0, 0xFF, 1, new int[] { 1 }, new byte[][] { new byte[128] }, null));

		assertThrows(ImdFormatException.class, () -> ImdImage.parse(data));
	}

	@Test
	public void rejectsASectorCylinderOrHeadMap() {
		ByteArrayOutputStream raw = new ByteArrayOutputStream();
		raw.write(0); // mode
		raw.write(0); // cylinder
		raw.write(0x80); // head byte with the cylinder-map-present flag set
		raw.write(1); // one sector
		raw.write(0); // size code 0 = 128 bytes
		byte[] data = image("comment", raw.toByteArray());

		assertThrows(ImdFormatException.class, () -> ImdImage.parse(data));
	}

	@Test
	public void rejectsAnUnknownSectorDataRecordType() {
		byte[] data = image("comment", track(0, 0, 0, 9, new int[] { 1 }, new byte[][] { new byte[128] }, null));

		assertThrows(ImdFormatException.class, () -> ImdImage.parse(data));
	}

	@Test
	public void rejectsATruncatedTrackRecord() {
		byte[] data = image("comment", new byte[] { 0, 0, 0, 1, 0 }); // declares 1 sector, then nothing follows

		assertThrows(ImdFormatException.class, () -> ImdImage.parse(data));
	}
}
