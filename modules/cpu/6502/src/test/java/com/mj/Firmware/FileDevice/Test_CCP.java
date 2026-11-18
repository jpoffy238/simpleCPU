package com.mj.Firmware.FileDevice;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.mj.Devices.AddressRange;
import com.mj.Devices.DeviceBus;
import com.mj.Devices.PBus;
import com.mj.Devices.VIA6522;
import com.mj.Disk.CpmFileSystem;
import com.mj.Disk.FloppyController;
import com.mj.Disk.EightInch.IBM3740;
import com.mj.Firmware.Framework.cpu001decoder;
import com.mj.cpu001.CPU;
import com.mj.memoryInterface.basicMemory;
import com.mj.memoryInterface.basicROM;
import com.mj.Video.VideoController;

/** Drives the real, cl65-compiled ccp.bin through a real CPU + Video, same
 * fixture shape as Test_BIOS/Test_Edit - boots it directly AS "CCP.COM"
 * (same trick Test_Edit's bootEditDirectly() uses) so each test only needs
 * MONITOR's B command, not a from-scratch BDOS boot sequence of its own.
 *
 * ccp.bin isn't part of the Maven build - see Test_Edit's class comment for
 * why - so every test here skips rather than fails if it hasn't been built
 * yet. */
public class Test_CCP {
	@TempDir
	Path tempDir;

	private PBus bus;
	private VideoController video;
	private CPU c;
	private CpmFileSystem fs;

	private static Path ccpBin() {
		return Paths.get("src/main/c/ccp.bin");
	}

	@BeforeEach
	public void setup() throws Exception {
		Assumptions.assumeTrue(Files.exists(ccpBin()),
				"ccp.bin not built - run `make` in modules/cpu/6502/src/main/c first");

		bus = new DeviceBus();
		bus.registerDevice(new basicMemory(bus, new AddressRange(0, 32 * 1024), null, 0));
		bus.registerDevice(new basicROM(bus, new AddressRange(0xf000, 0xf7ff), "BIOS.hex", 0));
		bus.registerDevice(new basicROM(bus, new AddressRange(0xf800, 0xffff), "MONITOR.hex", 0));

		bus.registerDevice(new basicMemory(bus, new AddressRange(0x8000, 0x81ff), null, 0));
		basicROM bdos = new basicROM(bus, new AddressRange(0xa000, 0xb7ff), null, 0);
		bdos.loadBinary("bdos.bin");
		bus.registerDevice(bdos);

		VIA6522 via = new VIA6522(bus, new AddressRange(0x9000, 0x900f));
		bus.registerDevice(via);

		basicMemory videoRam = new basicMemory(bus, new AddressRange(0xb800, 0xbfff), null, 0);
		bus.registerDevice(videoRam);
		basicMemory graphicsRam = new basicMemory(bus, new AddressRange(0xc000, 0xdfff), null, 0);
		bus.registerDevice(graphicsRam);
		video = new VideoController(new AddressRange(0x9020, 0x9024), videoRam, graphicsRam);
		bus.registerDevice(video);

		FloppyController fdc = new FloppyController(() -> via.requestInterrupt(VIA6522.FLAG_CB1), new AddressRange(0x9010, 0x9014));
		bus.registerDevice(fdc);
		IBM3740 drive0 = new IBM3740();
		drive0.setVirtualDiskFile(tempDir.resolve("drive0.img").toString());
		fdc.attachDrive(0, drive0);
		IBM3740 drive1 = new IBM3740();
		drive1.setVirtualDiskFile(tempDir.resolve("drive1.img").toString());
		fdc.attachDrive(1, drive1);

		fs = new CpmFileSystem(drive0);
		c = new CPU(bus, new cpu001decoder());
	}

	private void bootCcp(byte[] ccpProgram) throws Exception {
		fs.createFile("CCP.COM", ccpProgram);
		video.type("B\r");
	}

	private void runBriefly() throws Exception {
		c.start();
		Thread.sleep(3000);
		c.interrupt();
	}

	@Test
	public void statReportsTotalUsedAndFreeBlocksMatchingTheHostSideReference() throws Exception {
		byte[] ccpProgram = Files.readAllBytes(ccpBin());
		fs.format();
		bootCcp(ccpProgram);
		// CCP.COM itself is now a real file taking up at least one block, so
		// the expected free/used counts below have to come from the same
		// host-side reference (CpmFileSystem) rather than an assumption like
		// "everything's still free" - this cross-checks the C port's
		// block_bitmap scan (bdos_free_blocks()) against the validated Java
		// implementation's own usedBlocks()/totalBlocks().
		int totalBlocks = fs.totalBlockCount();
		int freeBlocks = fs.freeBlockCount();
		int usedBlocks = fs.usedBlockCount();

		video.type("STAT\r");
		runBriefly();

		String output = findRowContaining("k total");
		assertTrue(output != null, "STAT output not found on screen");
		assertTrue(output.contains(usedBlocks + "k used"), "got: [" + output + "]");
		assertTrue(output.contains(freeBlocks + "k free"), "got: [" + output + "]");
		assertTrue(output.contains(totalBlocks + "k total"), "got: [" + output + "]");
	}

	@Test
	public void bdosFreeBlocksPrimitiveReturnsTheRightCountDirectly() throws Exception {
		// Bypasses ccp.c/cmd_stat() entirely - JSR straight to BDOS_FREE_BLOCKS
		// ($A01E) and stash its 16-bit A(low)/X(high) return at $2000/$2001, to
		// isolate whether a wrong STAT result comes from bdos.c's own logic or
		// from cmd_stat()'s cc65-compiled use of it.
		byte[] program = {
				0x20, 0x1E, (byte) 0xA0,	// JSR $A01E (BDOS_FREE_BLOCKS)
				(byte) 0x8D, 0x00, 0x20,	// STA $2000
				(byte) 0x8E, 0x01, 0x20,	// STX $2001
				0x60				// RTS
		};
		fs.format();
		fs.createFile("CCP.COM", program);
		int expectedFree = fs.freeBlockCount();

		video.type("B\r");
		runBriefly();

		int low = c.bus.read(0x2000) & 0xff;
		int high = c.bus.read(0x2001) & 0xff;
		assertTrue(expectedFree == (low | (high << 8)),
				"expected " + expectedFree + ", got low=" + low + " high=" + high);
	}

	private String findRowContaining(String substring) throws Exception {
		for (int row = 0; row <= 23; row++) {
			String line = video.getRow(row);
			if (line.contains(substring)) {
				return line;
			}
		}
		return null;
	}
}
