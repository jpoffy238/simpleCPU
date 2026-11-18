package com.mj.z80;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Exercises the z80-asm file format (see Z80BinaryFile's class comment)
 * against hand-built byte arrays matching its documented layout exactly -
 * cross-checked by hand against a real z80-asm run (assembling
 * programs/example.asm in ~/git/z80asm/z80-asm-2.4.1 produces a file with
 * this identical header/address/data structure). Writes each fixture to a
 * real file via @TempDir rather than a classpath resource, exercising
 * readAllBytes()'s filesystem-fallback path directly. */
public class Test_Z80BinaryFile {
	@TempDir
	Path tempDir;

	private byte[] validFile(int startAddress, byte[] data) {
		byte[] file = new byte[10 + data.length];
		byte[] header = { 'Z', '8', '0', 'A', 'S', 'M', 0x1a, 0x0a };
		System.arraycopy(header, 0, file, 0, header.length);
		file[8] = (byte) (startAddress & 0xff);
		file[9] = (byte) ((startAddress >> 8) & 0xff);
		System.arraycopy(data, 0, file, 10, data.length);
		return file;
	}

	private String writeFixture(byte[] bytes) throws IOException {
		Path p = tempDir.resolve("fixture.z80");
		Files.write(p, bytes);
		return p.toString();
	}

	@Test
	public void readReturnsTheEmbeddedStartAddressAndData() throws Exception {
		byte[] data = { 0x3E, 0x05, 0x76 }; // LD A,5 / HALT
		String path = writeFixture(validFile(0x8000, data));

		Z80BinaryFile.Contents contents = Z80BinaryFile.read(path);

		assertEquals(0x8000, contents.startAddress);
		assertArrayEquals(data, contents.data);
	}

	@Test
	public void startAddressIsDecodedLittleEndian() throws Exception {
		String path = writeFixture(validFile(0x1234, new byte[] { 0x00 }));

		assertEquals(0x1234, Z80BinaryFile.read(path).startAddress);
	}

	@Test
	public void missingHeaderIsRejectedNotSwallowed() throws Exception {
		byte[] bogus = "NOT A Z80 FILE AT ALLxx".getBytes();
		String path = writeFixture(bogus);

		assertThrows(IOException.class, () -> Z80BinaryFile.read(path));
	}

	@Test
	public void tooShortToContainAStartAddressIsRejected() throws Exception {
		String path = writeFixture(new byte[] { 'Z', '8', '0', 'A', 'S', 'M', 0x1a, 0x0a });

		assertThrows(IOException.class, () -> Z80BinaryFile.read(path));
	}

	@Test
	public void missingFileIsReportedNotSwallowed() {
		assertThrows(IOException.class, () -> Z80BinaryFile.read(tempDir.resolve("no_such_file.z80").toString()));
	}

	private Z80Machine machine;

	@BeforeEach
	public void setup() {
		machine = new Z80Machine();
	}

	@Test
	public void loadWritesEveryByteOntoTheBusStartingAtItsEmbeddedAddress() throws Exception {
		byte[] data = { 0x11, 0x22, 0x33, 0x44 };
		String path = writeFixture(validFile(0x4000, data));

		Z80BinaryFile.load(machine.memoryBus(), path);

		for (int i = 0; i < data.length; i++) {
			assertEquals(data[i], machine.memoryBus().read(0x4000 + i), "byte at offset " + i);
		}
	}

	@Test
	public void machineLoadIntoMemoryIsEquivalentToLoadOntoItsOwnMemoryBus() throws Exception {
		byte[] data = { (byte) 0x3E, 0x2A }; // LD A,42
		String path = writeFixture(validFile(0x0200, data));

		machine.loadIntoMemory(path);

		assertEquals(0x3E, machine.memoryBus().read(0x0200) & 0xff);
		assertEquals(0x2A, machine.memoryBus().read(0x0201) & 0xff);
	}
}
