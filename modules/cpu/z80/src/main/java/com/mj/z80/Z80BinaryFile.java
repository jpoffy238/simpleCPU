package com.mj.z80;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;

import com.mj.Devices.PBus;
import com.mj.exceptions.DeviceUnavailable;
import com.mj.exceptions.ROException;
import com.mj.exceptions.illegalAddressException;

/** Loads binaries produced by the z80-asm toolchain (see the project's
 * reference-z80asm-toolchain notes for where that tool lives) - its own
 * small, self-describing file format, distinct from both Intel HEX (the
 * 6502 side's hand-assembled BIOS.hex/MONITOR.hex) and cc65's headerless
 * raw .bin (which needs its destination address supplied externally, since
 * that format carries none of its own). Documented in z80-asm's own
 * doc/man/z80-file.5:
 *
 *   offset 0-5   literal ASCII "Z80ASM"
 *   offset 6     0x1a
 *   offset 7     0x0a
 *   offset 8-9   16-bit start address, little-endian
 *   offset 10+   raw code/data bytes (length = file size - 10)
 *
 * Classpath-first lookup, same convention as
 * AbstractMemoryLayer.loadBinary() (modules/Devices) - lets a build copy a
 * .z80 file into src/*\/resources the same way the asm Makefile does for
 * .hex files, rather than depending on the JVM's working directory. */
public final class Z80BinaryFile {
	private static final byte[] HEADER = { 'Z', '8', '0', 'A', 'S', 'M', 0x1a, 0x0a };

	private Z80BinaryFile() {
	}

	/** The destination address and code/data one .z80 file carries - read but
	 * not yet written anywhere; see load() to actually write it onto a bus. */
	public static final class Contents {
		public final int startAddress;
		public final byte[] data;

		private Contents(int startAddress, byte[] data) {
			this.startAddress = startAddress;
			this.data = data;
		}
	}

	public static Contents read(String fileName) throws IOException {
		byte[] raw = readAllBytes(fileName);
		if (raw.length < HEADER.length + 2) {
			throw new IOException(fileName + ": too short to be a z80-asm file");
		}
		for (int i = 0; i < HEADER.length; i++) {
			if (raw[i] != HEADER[i]) {
				throw new IOException(fileName + ": missing z80-asm header - not assembled by z80-asm?");
			}
		}
		int startAddress = (raw[8] & 0xff) | ((raw[9] & 0xff) << 8);
		byte[] data = Arrays.copyOfRange(raw, HEADER.length + 2, raw.length);
		return new Contents(startAddress, data);
	}

	/** Reads fileName and writes its data onto bus starting at its own
	 * embedded start address, one byte at a time - unlike loadBinary()'s
	 * direct array copy into a single device's backing array, this goes
	 * through the bus itself, since (unlike a plain cc65 .bin) this format's
	 * destination address is self-describing rather than something the
	 * caller already has to know ahead of time to pick the right device. */
	public static void load(PBus bus, String fileName)
			throws IOException, illegalAddressException, ROException, DeviceUnavailable {
		Contents contents = read(fileName);
		for (int i = 0; i < contents.data.length; i++) {
			bus.write(contents.startAddress + i, contents.data[i]);
		}
	}

	private static byte[] readAllBytes(String fileName) throws IOException {
		InputStream resource = Z80BinaryFile.class.getClassLoader().getResourceAsStream(fileName);
		if (resource != null) {
			byte[] data = resource.readAllBytes();
			resource.close();
			return data;
		}
		return Files.readAllBytes(Paths.get(fileName));
	}
}
