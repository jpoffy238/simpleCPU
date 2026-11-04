package com.mj.Firmware.FileDevice;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.mj.Devices.AddressRange;
import com.mj.Devices.ConsoleDevice;
import com.mj.Devices.DeviceBus;
import com.mj.Devices.PBus;
import com.mj.Devices.VIA6522;
import com.mj.Disk.FloppyController;
import com.mj.Disk.EightInch.IBM3740;
import com.mj.Firmware.Framework.cpu001decoder;
import com.mj.cpu001.CPU;
import com.mj.memoryInterface.basicMemory;
import com.mj.memoryInterface.basicROM;

/** Drives the real BIOS.hex/MONITOR.hex ROMs through a CPU + console, the same way a
 * person typing at the keyboard would, and checks the result through getRow()/the bus
 * rather than the CPU's internal state - this is testing the firmware, not the CPU. */
public class Test_BIOS {
	@TempDir
	Path tempDir;

	private PBus bus;
	private ConsoleDevice console;
	private FloppyController fdc;
	private IBM3740 drive0;
	private IBM3740 drive1;
	private CPU c;

	@BeforeEach
	public void setup() throws Exception {
		bus = new DeviceBus();
		bus.registerDevice(new basicMemory(bus, new AddressRange(0, 32 * 1024), null, 0));
		// Two ROM chips, same as CPUMain - BIOS.hex (primitives) and MONITOR.hex (the
		// command interpreter, which carries its own vector table including the
		// project-specific fault vector, so no setVectors()/setFaultVector() call here).
		bus.registerDevice(new basicROM(bus, new AddressRange(0xf000, 0xf7ff), "BIOS.hex", 0));
		bus.registerDevice(new basicROM(bus, new AddressRange(0xf800, 0xffff), "MONITOR.hex", 0));

		VIA6522 via = new VIA6522(bus, new AddressRange(0x9000, 0x900f));
		bus.registerDevice(via);

		console = new ConsoleDevice(via, new AddressRange(0xec00, 0xec01));
		bus.registerDevice(console);

		fdc = new FloppyController(via, new AddressRange(0x9010, 0x9014));
		bus.registerDevice(fdc);
		drive0 = new IBM3740();
		drive0.setVirtualDiskFile(tempDir.resolve("drive0.img").toString());
		fdc.attachDrive(0, drive0);
		drive1 = new IBM3740();
		drive1.setVirtualDiskFile(tempDir.resolve("drive1.img").toString());
		fdc.attachDrive(1, drive1);

		c = new CPU(bus, new cpu001decoder());
	}

	private void runBriefly() throws Exception {
		c.start();
		Thread.sleep(2000);
		c.interrupt();
	}

	@Test
	public void dumpPrintsHexdumpStyleOutput() throws Exception {
		c.bus.write(0x2000, (byte) 'H');
		c.bus.write(0x2001, (byte) 'I');

		console.type("D 2000\r");
		runBriefly();

		assertTrue(console.getRow(0).contains("D 2000"), "the typed command should be echoed");
		assertTrue(console.getRow(1).startsWith(
				"2000  48 49 00 00 00 00 00 00  00 00 00 00 00 00 00 00  |HI..............|"),
				"first dump line, got: [" + console.getRow(1) + "]");
		assertTrue(console.getRow(16).startsWith("20F0"), "16 lines of 16 bytes covers $2000-$20FF");
	}

	@Test
	public void writeStoresBytesStartingAtTheGivenAddress() throws Exception {
		console.type("W 2000 48 49 4a\r");
		runBriefly();

		assertEquals(0x48, c.bus.read(0x2000) & 0xff);
		assertEquals(0x49, c.bus.read(0x2001) & 0xff);
		assertEquals(0x4a, c.bus.read(0x2002) & 0xff);
	}

	@Test
	public void lineFeedAloneAlsoTerminatesTheLine() throws Exception {
		// Swing's keyTyped for Enter isn't consistently '\r' across platforms - a real
		// interactive run got stuck here waiting for a '\r' that never arrived.
		console.type("W 2000 48\n");
		runBriefly();

		assertEquals(0x48, c.bus.read(0x2000) & 0xff);
	}

	@Test
	public void dollarPrefixIsAcceptedOnAddressesAndBytes() throws Exception {
		console.type("W $2000 $48 $49\r");
		runBriefly();

		assertEquals(0x48, c.bus.read(0x2000) & 0xff, "a bare '$' shouldn't make the address default to 0");
		assertEquals(0x49, c.bus.read(0x2001) & 0xff);
	}

	@Test
	public void unmappedMemoryAccessFaultsAndRecoversToThePrompt() throws Exception {
		c.bus.write(0x2000, (byte) 'H');

		// $A000 has no device registered on this bus - dumping it should fault partway
		// through the first line instead of taking down the whole emulated machine.
		console.type("D A000\r");
		console.type("D 2000\r");
		runBriefly();

		assertTrue(console.getRow(1).contains("A000"), "the dump should start before faulting");
		assertTrue(console.getRow(1).contains("?"), "the fault handler's error marker");

		boolean recovered = false;
		for (int row = 0; row <= 18; row++) {
			if (console.getRow(row).startsWith("2000")) {
				recovered = true;
				break;
			}
		}
		assertTrue(recovered, "the monitor should still be alive and process a command after the fault");
	}

	@Test
	public void backspaceErasesTheLastTypedCharacter() throws Exception {
		// Type a wrong byte value, backspace both digits away, then type the right one.
		console.type("W 2000 99\b\b48\r");
		runBriefly();

		assertEquals(0x48, c.bus.read(0x2000) & 0xff,
				"the erased '99' shouldn't have reached the parser at all");
	}

	@Test
	public void backspaceAtTheStartOfTheLineIsIgnored() throws Exception {
		console.type("\b\bD 2000\r");
		runBriefly();

		assertTrue(console.getRow(1).startsWith("2000"),
				"leading backspaces with nothing to erase shouldn't break the command");
	}

	@Test
	public void helpListsTheAvailableCommands() throws Exception {
		console.type("H\r");
		runBriefly();

		assertTrue(console.getRow(1).contains("D addr") && console.getRow(1).contains("dump"),
				"got: [" + console.getRow(1) + "]");
		assertTrue(console.getRow(2).contains("W addr") && console.getRow(2).contains("write"),
				"got: [" + console.getRow(2) + "]");
		assertTrue(console.getRow(3).contains("H") && console.getRow(3).contains("help"),
				"got: [" + console.getRow(3) + "]");
		assertTrue(console.getRow(5).contains("S drive") && console.getRow(5).contains("select"),
				"got: [" + console.getRow(5) + "]");
		assertTrue(console.getRow(6).contains("F [fill]") && console.getRow(6).contains("format"),
				"got: [" + console.getRow(6) + "]");
		assertTrue(console.getRow(7).contains("L track") && console.getRow(7).contains("load"),
				"got: [" + console.getRow(7) + "]");
		assertTrue(console.getRow(8).contains("P track") && console.getRow(8).contains("write"),
				"got: [" + console.getRow(8) + "]");
		assertTrue(console.getRow(9).contains("B") && console.getRow(9).contains("boot"),
				"got: [" + console.getRow(9) + "]");
	}

	@Test
	public void executeRunsAPokedRoutineAndReturnsToThePrompt() throws Exception {
		// LDA #$99 / STA $2000 / RTS - a tiny subroutine for E to call via JSR.
		int addr = 0x3000;
		c.bus.write(addr, (byte) 0xA9);
		c.bus.write(addr + 1, (byte) 0x99);
		c.bus.write(addr + 2, (byte) 0x8D);
		c.bus.write(addr + 3, (byte) 0x00);
		c.bus.write(addr + 4, (byte) 0x20);
		c.bus.write(addr + 5, (byte) 0x60);

		console.type("E 3000\r");
		console.type("D 2000\r");
		runBriefly();

		assertEquals(0x99, c.bus.read(0x2000) & 0xff, "the executed routine should have run");

		boolean recovered = false;
		for (int row = 0; row <= 18; row++) {
			if (console.getRow(row).startsWith("2000")) {
				recovered = true;
				break;
			}
		}
		assertTrue(recovered, "the monitor should still be alive after E's routine returns via RTS");
	}

	@Test
	public void formatFillsEveryTrackOfTheSelectedDriveWithTheGivenByte() throws Exception {
		console.type("S 0\r");
		console.type("F AA\r");
		runBriefly();

		drive0.setTrack(0);
		drive0.setSector(1);
		byte[] firstTrack = drive0.read();
		drive0.setTrack(75);
		drive0.setSector(26);
		byte[] lastSectorOfLastTrack = drive0.read();

		for (byte b : firstTrack) {
			assertEquals((byte) 0xAA, b, "track 0 should have been formatted with the given fill byte");
		}
		for (byte b : lastSectorOfLastTrack) {
			assertEquals((byte) 0xAA, b, "the last track should also have been formatted");
		}
	}

	@Test
	public void formatWithNoArgumentDefaultsToZero() throws Exception {
		console.type("S 0\r");
		console.type("F\r");
		runBriefly();

		for (byte b : drive0.read()) {
			assertEquals(0, b, "no fill byte given should default to $00");
		}
	}

	@Test
	public void loadReadsASectorFromTheSelectedDriveIntoMemory() throws Exception {
		byte[] pattern = new byte[128];
		for (int i = 0; i < pattern.length; i++) {
			pattern[i] = (byte) (0x30 + i);
		}
		drive0.setTrack(10);
		drive0.setSector(5);
		drive0.write(pattern);

		console.type("S 0\r");
		console.type("L 0A 05 3000\r");
		runBriefly();

		for (int i = 0; i < pattern.length; i++) {
			assertEquals(pattern[i], c.bus.read(0x3000 + i), "mismatch at offset " + i);
		}
	}

	@Test
	public void putWritesASectorFromMemoryToTheSelectedDrive() throws Exception {
		byte[] pattern = new byte[128];
		for (int i = 0; i < pattern.length; i++) {
			pattern[i] = (byte) (0x60 + i);
		}
		for (int i = 0; i < pattern.length; i++) {
			c.bus.write(0x4000 + i, pattern[i]);
		}

		console.type("S 0\r");
		console.type("P 0B 06 4000\r");
		runBriefly();

		drive0.setTrack(11);
		drive0.setSector(6);
		assertArrayEquals(pattern, drive0.read());
	}

	@Test
	public void selectingADifferentDriveUsesAnIndependentDisk() throws Exception {
		byte[] pattern = new byte[128];
		for (int i = 0; i < pattern.length; i++) {
			pattern[i] = (byte) (0x11 + i);
			c.bus.write(0x5000 + i, pattern[i]);
		}

		console.type("S 1\r");
		console.type("P 02 03 5000\r");
		runBriefly();

		drive1.setTrack(2);
		drive1.setSector(3);
		assertArrayEquals(pattern, drive1.read(), "drive 1 should have the written pattern");

		drive0.setTrack(2);
		drive0.setSector(3);
		for (byte b : drive0.read()) {
			assertEquals(0, b, "drive 0 shouldn't have been touched by a write to drive 1");
		}
	}

	@Test
	public void bootLoadsTrackZeroSectorOneAndJumpsToIt() throws Exception {
		// LDA #$99 / STA $2000 / RTS - the same tiny subroutine the E test uses, but
		// planted directly on the "disk" at track 0, sector 1, where B expects it.
		byte[] bootSector = new byte[128];
		bootSector[0] = (byte) 0xA9;
		bootSector[1] = (byte) 0x99;
		bootSector[2] = (byte) 0x8D;
		bootSector[3] = (byte) 0x00;
		bootSector[4] = (byte) 0x20;
		bootSector[5] = (byte) 0x60;
		drive0.setTrack(0);
		drive0.setSector(1);
		drive0.write(bootSector);

		console.type("B\r");
		console.type("D 2000\r");
		runBriefly();

		assertEquals(0x99, c.bus.read(0x2000) & 0xff, "the booted routine should have run");

		boolean recovered = false;
		for (int row = 0; row <= 18; row++) {
			if (console.getRow(row).startsWith("2000")) {
				recovered = true;
				break;
			}
		}
		assertTrue(recovered, "the monitor should still be alive after the boot routine returns via RTS");
	}

	@Test
	public void bootedProgramCanPrintThroughTheBiosPutchPrimitive() throws Exception {
		// LDA #char / JSR BIOS_PUTCH ($F000), repeated per character, then RTS - a
		// boot sector that actually calls back into the primitives ROM rather than
		// just poking memory directly, proving out the whole B -> BIOS_READ_SECTOR ->
		// JSR BOOTADDR -> BIOS_PUTCH round trip with a real (if tiny) loaded program.
		int[] program = {
				0xA9, 0x48, 0x20, 0x00, 0xF0,	// LDA #'H' / JSR BIOS_PUTCH
				0xA9, 0x65, 0x20, 0x00, 0xF0,	// LDA #'e' / JSR BIOS_PUTCH
				0xA9, 0x6C, 0x20, 0x00, 0xF0,	// LDA #'l' / JSR BIOS_PUTCH
				0xA9, 0x6C, 0x20, 0x00, 0xF0,	// LDA #'l' / JSR BIOS_PUTCH
				0xA9, 0x6F, 0x20, 0x00, 0xF0,	// LDA #'o' / JSR BIOS_PUTCH
				0x60							// RTS
		};
		byte[] bootSector = new byte[128];
		for (int i = 0; i < program.length; i++) {
			bootSector[i] = (byte) program[i];
		}
		drive0.setTrack(0);
		drive0.setSector(1);
		drive0.write(bootSector);

		console.type("B\r");
		runBriefly();

		assertTrue(console.getRow(1).contains("Hello"), "got: [" + console.getRow(1) + "]");
	}

	@Test
	public void resetHomesTheDriveAndTrackRegardlessOfPriorState() throws Exception {
		// Simulate a previous program (a future DOS, say) leaving the hardware
		// selected to a different drive/track - direct register pokes, bypassing
		// the monitor entirely, standing in for "whatever ran before this reset".
		// c hasn't been started yet, so these pokes land before the CPU's own
		// first-ever reset, which is exactly what we want to test against.
		c.bus.write(0x9014, (byte) 1); // FD_DRIVE
		c.bus.write(0x9011, (byte) 5); // FD_TRACK

		runBriefly();

		assertEquals(0, c.bus.read(0x9014) & 0xff, "reset should re-select drive 0");
		assertEquals(0, c.bus.read(0x9011) & 0xff, "reset should restore to track 0");
	}
}
