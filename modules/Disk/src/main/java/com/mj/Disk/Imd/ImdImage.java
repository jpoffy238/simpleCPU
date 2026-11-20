package com.mj.Disk.Imd;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import com.mj.Disk.exceptions.ImdFormatException;

/** Decodes the ImageDisk (.IMD) format - a real preservation tool (www.dunfield.com)
 * widely used to archive vintage floppies, including actual period CP/M disks (see
 * CpmFileSystem's own class comment for this project's in-house flat-file format,
 * which an .IMD file is NOT - this is a different, real-world format entirely).
 * <p>
 * Deliberately CP/M-agnostic - this class only turns the file's bytes into tracks
 * and sectors, nothing about CP/M's own directory/FCB layout. A real disk can (and
 * the period CP/M disks this project has actually tried do) mix sector sizes within
 * one image - a single-density 128-byte boot track followed by double-density
 * tracks at a larger size - so toFlatBytes() only concatenates each track's sectors
 * in logical order; deblocking a larger physical sector into CP/M's fixed 128-byte
 * logical records is a CP/M-specific concern for a later class, not this one.
 * <p>
 * Format (IMD 1.x, the only version seen in practice): an ASCII comment terminated
 * by 0x1A, then one variable-length track record per track until end of file - see
 * readTrack() for the per-track layout. Sector data within a track is stored in
 * physical disk order (whatever the sector numbering map says, including real
 * interleave/skew if a drive ever used it), not sorted by logical number - callers
 * needing logical order (Track.sectorNumbers() is sorted) get it from that method,
 * not from assuming storage order. */
public class ImdImage {

	/** sectorSizeCode -> byte size, straight from the IMD spec. 0xFF (variable size,
	 * an explicit per-sector length table) is real but not supported - no image this
	 * project has tried uses it, and guessing at an unverified format risks silently
	 * misreading sector boundaries rather than failing loudly. */
	private static final int[] SECTOR_SIZE_FOR_CODE = { 128, 256, 512, 1024, 2048, 4096, 8192 };

	/** One physical track: a fixed cylinder/head and (for every sector on it) a fixed
	 * size - real mixed-density disks vary this across tracks, never within one. */
	public static final class Track {
		private final int cylinder;
		private final int head;
		private final int sectorSize;
		private final Map<Integer, byte[]> sectorsByNumber;

		private Track(int cylinder, int head, int sectorSize, Map<Integer, byte[]> sectorsByNumber) {
			this.cylinder = cylinder;
			this.head = head;
			this.sectorSize = sectorSize;
			this.sectorsByNumber = sectorsByNumber;
		}

		public int cylinder() {
			return cylinder;
		}

		public int head() {
			return head;
		}

		public int sectorSize() {
			return sectorSize;
		}

		/** The logical sector numbers actually present on this track, ascending - not
		 * necessarily a contiguous 1..N range (a real drive can have missing/skipped
		 * sector numbers), and not the physical storage order (see the class comment). */
		public SortedSet<Integer> sectorNumbers() {
			return Collections.unmodifiableSortedSet(new TreeSet<Integer>(sectorsByNumber.keySet()));
		}

		/** Null if this sector was recorded as unavailable (IMD data-record type 0 -
		 * a real read error at capture time), not a zero-filled placeholder - a caller
		 * that cares about the distinction (toFlatBytes() doesn't) can tell the two
		 * apart this way. */
		public byte[] sector(int logicalSectorNumber) {
			byte[] data = sectorsByNumber.get(logicalSectorNumber);
			return data == null ? null : data.clone();
		}
	}

	private final String comment;
	private final List<Track> tracks;

	private ImdImage(String comment, List<Track> tracks) {
		this.comment = comment;
		this.tracks = tracks;
	}

	public static ImdImage read(Path path) throws IOException, ImdFormatException {
		return parse(Files.readAllBytes(path));
	}

	/** Exposed directly (not just via read()) so tests can build small synthetic .IMD
	 * byte arrays in-process rather than needing a real file fixture on disk. */
	public static ImdImage parse(byte[] data) throws ImdFormatException {
		int commentEnd = indexOf(data, (byte) 0x1A);
		if (commentEnd < 0) {
			throw new ImdFormatException("No comment terminator (0x1A) found - not an IMD file");
		}
		String comment = new String(data, 0, commentEnd, java.nio.charset.StandardCharsets.US_ASCII);

		Cursor cursor = new Cursor(data, commentEnd + 1);
		List<Track> tracks = new ArrayList<Track>();
		while (cursor.hasMore()) {
			tracks.add(readTrack(cursor));
		}
		return new ImdImage(comment, tracks);
	}

	public String comment() {
		return comment;
	}

	/** In the order the file stored them - normally cylinder-ascending for a real
	 * single-pass capture, but not guaranteed (a tool could append a re-read retry of
	 * an earlier track). Use tracksByPosition() for a guaranteed (cylinder, head) order. */
	public List<Track> tracks() {
		return Collections.unmodifiableList(tracks);
	}

	/** Every track, sorted by (cylinder, head) ascending - the order a real drive's
	 * heads would encounter them, and so the order toFlatBytes() concatenates them in. */
	public List<Track> tracksByPosition() {
		Map<Long, Track> byPosition = new TreeMap<Long, Track>();
		for (Track track : tracks) {
			byPosition.put(((long) track.cylinder << 32) | (track.head & 0xffffffffL), track);
		}
		return new ArrayList<Track>(byPosition.values());
	}

	/** Concatenates every track's sectors, track-major (cylinder/head ascending, via
	 * tracksByPosition()), each track's own sectors in ascending logical-number order -
	 * not physical storage order, matching how a real sequential read of the disk
	 * would present the data once the controller's own sector interleave is undone.
	 * An unavailable sector (Track.sector() returning null) is substituted with a
	 * zero-filled sector of that track's own size, so this never leaves a gap. */
	public byte[] toFlatBytes() {
		List<Track> ordered = tracksByPosition();
		int total = 0;
		for (Track track : ordered) {
			total += track.sectorNumbers().size() * track.sectorSize();
		}
		byte[] result = new byte[total];
		int offset = 0;
		for (Track track : ordered) {
			for (int number : track.sectorNumbers()) {
				byte[] sector = track.sector(number);
				if (sector != null) {
					System.arraycopy(sector, 0, result, offset, track.sectorSize());
				}
				offset += track.sectorSize();
			}
		}
		return result;
	}

	private static Track readTrack(Cursor cursor) throws ImdFormatException {
		cursor.readByte(); // mode - recording rate/encoding, irrelevant once decoded to bytes
		int cylinder = cursor.readByte();
		int headByte = cursor.readByte();
		int sectorCount = cursor.readByte();
		int sizeCode = cursor.readByte();

		boolean cylinderMapPresent = (headByte & 0x80) != 0;
		boolean headMapPresent = (headByte & 0x40) != 0;
		if (cylinderMapPresent || headMapPresent) {
			throw new ImdFormatException(
					"Sector cylinder/head maps aren't supported (track at cylinder " + cylinder + ")");
		}
		int head = headByte & 0x3F;

		if (sizeCode < 0 || sizeCode >= SECTOR_SIZE_FOR_CODE.length) {
			throw new ImdFormatException("Unsupported sector size code " + sizeCode
					+ " (track at cylinder " + cylinder + ") - variable-size sector tables aren't supported");
		}
		int sectorSize = SECTOR_SIZE_FOR_CODE[sizeCode];

		int[] sectorNumberMap = new int[sectorCount];
		for (int i = 0; i < sectorCount; i++) {
			sectorNumberMap[i] = cursor.readByte();
		}

		Map<Integer, byte[]> sectorsByNumber = new LinkedHashMap<Integer, byte[]>();
		for (int i = 0; i < sectorCount; i++) {
			int recordType = cursor.readByte();
			byte[] sectorData;
			switch (recordType) {
				case 0: // unavailable - no data follows
					sectorData = null;
					break;
				case 1: case 3: case 5: case 7: // normal data, sectorSize bytes follow
					sectorData = cursor.readBytes(sectorSize);
					break;
				case 2: case 4: case 6: case 8: // compressed - one fill byte follows
					byte fill = (byte) cursor.readByte();
					sectorData = new byte[sectorSize];
					java.util.Arrays.fill(sectorData, fill);
					break;
				default:
					throw new ImdFormatException("Unknown sector data record type " + recordType
							+ " (track at cylinder " + cylinder + ")");
			}
			sectorsByNumber.put(sectorNumberMap[i], sectorData);
		}

		return new Track(cylinder, head, sectorSize, sectorsByNumber);
	}

	private static int indexOf(byte[] data, byte value) {
		for (int i = 0; i < data.length; i++) {
			if (data[i] == value) {
				return i;
			}
		}
		return -1;
	}

	/** A position-tracking cursor over the file's bytes - kept private/internal
	 * rather than exposing raw offset arithmetic throughout readTrack(). */
	private static final class Cursor {
		private final byte[] data;
		private int pos;

		Cursor(byte[] data, int pos) {
			this.data = data;
			this.pos = pos;
		}

		boolean hasMore() {
			return pos < data.length;
		}

		int readByte() throws ImdFormatException {
			if (pos >= data.length) {
				throw new ImdFormatException("Unexpected end of file while reading a track record");
			}
			return data[pos++] & 0xff;
		}

		byte[] readBytes(int count) throws ImdFormatException {
			if (pos + count > data.length) {
				throw new ImdFormatException("Unexpected end of file while reading a track record");
			}
			byte[] result = new byte[count];
			System.arraycopy(data, pos, result, 0, count);
			pos += count;
			return result;
		}
	}
}
