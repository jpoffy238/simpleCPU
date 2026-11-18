package com.mj.z80;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Path;
import java.util.Arrays;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.mj.Video.VideoController;

/** Proves out Z80VideoDiskMachine's actual bus wiring - Video and
 * FloppyController respond on the port bus at the addresses that class
 * documents, Video's bulk RAM round-trips on the memory bus, and a disk
 * image attached through attachDrive() round-trips a real sector through
 * FloppyController's port-bus registers end to end. */
public class Test_Z80VideoDiskMachine {
	@TempDir
	Path tempDir;

	private Z80VideoDiskMachine machine;

	@BeforeEach
	public void setup() {
		machine = new Z80VideoDiskMachine();
	}

	// ---- memory bus ----

	@Test
	public void ramRoundTripsOutsideTheVideoCarveOuts() throws Exception {
		machine.memoryBus().write(0x1000, (byte) 0x42);
		assertEquals(0x42, machine.memoryBus().read(0x1000) & 0xff);

		machine.memoryBus().write(0xf000, (byte) 0x99);
		assertEquals(0x99, machine.memoryBus().read(0xf000) & 0xff);
	}

	@Test
	public void charRamRoundTripsOnTheMemoryBus() throws Exception {
		machine.memoryBus().write(0xb800, (byte) 'A');
		assertEquals('A', machine.memoryBus().read(0xb800) & 0xff);
	}

	@Test
	public void graphicsRamRoundTripsOnTheMemoryBus() throws Exception {
		machine.memoryBus().write(0xc000, (byte) 0xff);
		assertEquals(0xff, machine.memoryBus().read(0xc000) & 0xff);
	}

	// ---- port bus: VideoController ----

	@Test
	public void videoCrtcRegistersRoundTripThroughThePortBus() throws Exception {
		machine.portBus().write(0x00, (byte) 1); // REG_ADDRESS - select CRTC register R1
		machine.portBus().write(0x01, (byte) 80); // REG_DATA

		assertEquals(1, machine.portBus().read(0x00) & 0xff);
		assertEquals(80, machine.portBus().read(0x01) & 0xff);
	}

	@Test
	public void videoModeRegisterRoundTripsThroughThePortBus() throws Exception {
		machine.portBus().write(0x04, (byte) VideoController.MODE_GRAPHICS);

		assertEquals(VideoController.MODE_GRAPHICS, machine.portBus().read(0x04) & 0xff);
	}

	// ---- port bus: FloppyController ----

	@Test
	public void floppyRegistersRoundTripThroughThePortBus() throws Exception {
		machine.portBus().write(0x11, (byte) 5); // REG_TRACK
		machine.portBus().write(0x14, (byte) 3); // REG_DRIVE_SELECT

		assertEquals(5, machine.portBus().read(0x11) & 0xff);
		assertEquals(3, machine.portBus().read(0x14) & 0xff);
	}

	@Test
	public void issuingACommandWithNoDriveAttachedReportsNotReady() throws Exception {
		machine.portBus().write(0x14, (byte) 0); // select drive 0 - nothing attached yet
		machine.portBus().write(0x10, (byte) 0x00); // Restore

		assertEquals(0x80, machine.portBus().read(0x10) & 0xff, "STATUS_NOT_READY");
	}

	@Test
	public void attachingADriveAndWritingThenReadingASectorRoundTripsThroughThePortBus() throws Exception {
		machine.attachDrive(0, tempDir.resolve("z80-disk0.img").toString());

		byte[] sector = new byte[128];
		Arrays.fill(sector, (byte) 0xAB);

		machine.portBus().write(0x14, (byte) 0); // REG_DRIVE_SELECT
		machine.portBus().write(0x11, (byte) 0); // REG_TRACK
		machine.portBus().write(0x12, (byte) 1); // REG_SECTOR
		machine.portBus().write(0x10, (byte) 0xA0); // Write Sector
		for (byte b : sector) {
			machine.portBus().write(0x13, b); // REG_DATA
		}
		assertEquals(0, machine.portBus().read(0x10) & (0x01 | 0x02), "BUSY/DRQ should have cleared once the whole sector was written");

		machine.portBus().write(0x10, (byte) 0x80); // Read Sector
		byte[] readBack = new byte[128];
		for (int i = 0; i < readBack.length; i++) {
			readBack[i] = (byte) machine.portBus().read(0x13); // REG_DATA
		}

		assertArrayEquals(sector, readBack);
	}
}
