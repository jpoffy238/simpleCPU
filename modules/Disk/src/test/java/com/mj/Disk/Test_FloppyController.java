package com.mj.Disk;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.mj.Devices.AddressRange;
import com.mj.Devices.DeviceBus;
import com.mj.Devices.PBus;
import com.mj.Devices.VIA6522;
import com.mj.Disk.EightInch.IBM3740;
import com.mj.exceptions.DeviceUnavailable;
import com.mj.exceptions.ROException;
import com.mj.exceptions.illegalAddressException;

/** Exercises the WD179x-style FloppyController register interface end to end,
 * through a real DeviceBus, against a file-backed IBM3740 drive. */
public class Test_FloppyController {
	private static final int BASE = 0x3000;
	private static final int REG_STATUS_COMMAND = BASE + FloppyController.REG_STATUS_COMMAND;
	private static final int REG_TRACK = BASE + FloppyController.REG_TRACK;
	private static final int REG_SECTOR = BASE + FloppyController.REG_SECTOR;
	private static final int REG_DATA = BASE + FloppyController.REG_DATA;
	private static final int REG_DRIVE_SELECT = BASE + FloppyController.REG_DRIVE_SELECT;

	private static final int CMD_RESTORE = 0x00;
	private static final int CMD_SEEK = 0x10;
	private static final int CMD_READ_SECTOR = 0x80;
	private static final int CMD_WRITE_SECTOR = 0xA0;
	private static final int CMD_FORMAT_TRACK = 0xF0;

	private static final int STATUS_BUSY = 0x01;
	private static final int STATUS_DRQ = 0x02;
	private static final int STATUS_RECORD_NOT_FOUND = 0x04;
	private static final int STATUS_NOT_READY = 0x80;

	@TempDir
	Path tempDir;

	private static final int VIA_BASE = 0x9000;

	private PBus bus;
	private VIA6522 via;
	private FloppyController controller;

	@BeforeEach
	public void setup() throws Exception {
		bus = new DeviceBus();
		via = new VIA6522(bus, new AddressRange(VIA_BASE, VIA_BASE + 0xF));
		bus.registerDevice(via);
		bus.write(VIA_BASE + VIA6522.REG_IER, (byte) (0x80 | VIA6522.FLAG_CB1));

		controller = new FloppyController(via, new AddressRange(BASE, BASE + 4));
		bus.registerDevice(controller);
	}

	private IBM3740 newDrive(String imageName) throws Exception {
		IBM3740 drive = new IBM3740();
		drive.setVirtualDiskFile(tempDir.resolve(imageName).toString());
		return drive;
	}

	private int status() throws illegalAddressException, DeviceUnavailable {
		return bus.read(REG_STATUS_COMMAND) & 0xff;
	}

	private byte[] samplePattern(int seed) {
		byte[] data = new byte[128];
		for (int i = 0; i < data.length; i++) {
			data[i] = (byte) (seed + i);
		}
		return data;
	}

	private void writeSector(byte[] data) throws illegalAddressException, ROException, DeviceUnavailable {
		bus.write(REG_STATUS_COMMAND, (byte) CMD_WRITE_SECTOR);
		assertEquals(STATUS_BUSY | STATUS_DRQ, status());
		for (int i = 0; i < data.length; i++) {
			bus.clearInterupt();
			bus.write(REG_DATA, data[i]);
			boolean lastByte = (i == data.length - 1);
			assertEquals(!lastByte, bus.IsInterruptRaised(),
					"interrupt expected after every byte except the last");
		}
		assertEquals(0, status(), "BUSY/DRQ should clear once the sector is flushed");
	}

	private byte[] readSector() throws illegalAddressException, ROException, DeviceUnavailable {
		bus.clearInterupt();
		bus.write(REG_STATUS_COMMAND, (byte) CMD_READ_SECTOR);
		assertTrue(bus.IsInterruptRaised(), "first byte should already be ready");
		assertEquals(STATUS_BUSY | STATUS_DRQ, status());

		byte[] result = new byte[128];
		for (int i = 0; i < result.length; i++) {
			bus.clearInterupt();
			result[i] = (byte) bus.read(REG_DATA);
			boolean lastByte = (i == result.length - 1);
			assertEquals(!lastByte, bus.IsInterruptRaised(),
					"interrupt expected after every byte except the last");
		}
		assertEquals(0, status(), "BUSY/DRQ should clear once the sector is fully read");
		return result;
	}

	@Test
	public void writeThenReadSectorRoundTripsThroughTheBackingFile() throws Exception {
		controller.attachDrive(0, newDrive("drive0.img"));

		bus.write(REG_TRACK, (byte) 5);
		bus.write(REG_SECTOR, (byte) 3);
		byte[] written = samplePattern(0x10);
		writeSector(written);

		bus.write(REG_TRACK, (byte) 5);
		bus.write(REG_SECTOR, (byte) 3);
		byte[] readBack = readSector();

		assertArrayEquals(written, readBack);
	}

	@Test
	public void differentSectorsOnTheSameDriveDoNotAlias() throws Exception {
		controller.attachDrive(0, newDrive("drive0.img"));

		bus.write(REG_TRACK, (byte) 1);
		bus.write(REG_SECTOR, (byte) 1);
		writeSector(samplePattern(1));

		bus.write(REG_TRACK, (byte) 1);
		bus.write(REG_SECTOR, (byte) 2);
		writeSector(samplePattern(99));

		bus.write(REG_TRACK, (byte) 1);
		bus.write(REG_SECTOR, (byte) 1);
		assertArrayEquals(samplePattern(1), readSector());
	}

	@Test
	public void driveSelectPicksAnIndependentBackingFile() throws Exception {
		controller.attachDrive(0, newDrive("drive0.img"));
		controller.attachDrive(1, newDrive("drive1.img"));

		bus.write(REG_DRIVE_SELECT, (byte) 0);
		bus.write(REG_TRACK, (byte) 0);
		bus.write(REG_SECTOR, (byte) 1);
		writeSector(samplePattern(0x40));

		bus.write(REG_DRIVE_SELECT, (byte) 1);
		bus.write(REG_TRACK, (byte) 0);
		bus.write(REG_SECTOR, (byte) 1);
		writeSector(samplePattern(0x80));

		bus.write(REG_DRIVE_SELECT, (byte) 0);
		assertArrayEquals(samplePattern(0x40), readSector());

		bus.write(REG_DRIVE_SELECT, (byte) 1);
		assertArrayEquals(samplePattern(0x80), readSector());
	}

	@Test
	public void seekAndRestoreMoveTheTrackRegister() throws Exception {
		controller.attachDrive(0, newDrive("drive0.img"));

		bus.write(REG_DATA, (byte) 40);
		bus.clearInterupt();
		bus.write(REG_STATUS_COMMAND, (byte) CMD_SEEK);
		assertEquals(40, bus.read(REG_TRACK) & 0xff);
		assertEquals(0, status());
		assertTrue(bus.IsInterruptRaised(), "Type I commands interrupt on completion");

		bus.clearInterupt();
		bus.write(REG_STATUS_COMMAND, (byte) CMD_RESTORE);
		assertEquals(0, bus.read(REG_TRACK) & 0xff);
		assertTrue(bus.IsInterruptRaised());
	}

	@Test
	public void invalidSectorSetsRecordNotFound() throws Exception {
		controller.attachDrive(0, newDrive("drive0.img"));

		bus.write(REG_TRACK, (byte) 0);
		bus.write(REG_SECTOR, (byte) 0); // sectors are numbered 1..26, 0 is invalid
		bus.clearInterupt();
		bus.write(REG_STATUS_COMMAND, (byte) CMD_READ_SECTOR);

		assertEquals(STATUS_RECORD_NOT_FOUND, status());
		assertTrue(bus.IsInterruptRaised());
	}

	@Test
	public void unattachedDriveReportsNotReadyWithoutInterrupting() throws Exception {
		bus.write(REG_SECTOR, (byte) 1);
		bus.clearInterupt();
		bus.write(REG_STATUS_COMMAND, (byte) CMD_READ_SECTOR);

		assertEquals(STATUS_NOT_READY, status());
		assertFalse(bus.IsInterruptRaised(), "nothing is actually happening, so nothing should interrupt");
	}

	@Test
	public void formatTrackFillsEverySectorOnTheCurrentTrackWithTheDataRegisterByte() throws Exception {
		IBM3740 drive = newDrive("drive0.img");
		controller.attachDrive(0, drive);

		bus.write(REG_TRACK, (byte) 7);
		bus.write(REG_DATA, (byte) 0xAA);
		bus.clearInterupt();
		bus.write(REG_STATUS_COMMAND, (byte) CMD_FORMAT_TRACK);

		assertEquals(0, status(), "format should complete synchronously, like Seek/Restore");
		assertTrue(bus.IsInterruptRaised(), "a single completion interrupt, Type-I style");

		for (int sector = 1; sector <= 26; sector++) {
			bus.write(REG_SECTOR, (byte) sector);
			byte[] data = readSector();
			for (byte b : data) {
				assertEquals((byte) 0xAA, b, "sector " + sector + " should have been formatted");
			}
		}
	}

	@Test
	public void formatTrackOnAnUnattachedDriveReportsNotReady() throws Exception {
		bus.write(REG_STATUS_COMMAND, (byte) CMD_FORMAT_TRACK);

		assertEquals(STATUS_NOT_READY, status());
	}
}
