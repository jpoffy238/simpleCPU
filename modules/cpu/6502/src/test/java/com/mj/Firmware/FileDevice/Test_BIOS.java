package com.mj.Firmware.FileDevice;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.Arrays;

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

/** Drives the real BIOS.hex/MONITOR.hex ROMs through a CPU + Video, the same way a
 * person typing at the keyboard would, and checks the result through getRow()/the bus
 * rather than the CPU's internal state - this is testing the firmware, not the CPU.
 * Video is the sole console (BIOS_PUTCH/BIOS_GETCH's full cutover from ConsoleDevice -
 * see BIOS.asm), so this fixture no longer registers one at all. */
public class Test_BIOS {
	@TempDir
	Path tempDir;

	private PBus bus;
	private VideoController video;
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

		// BDOS, same as CPUMain - see AbstractMemoryLayer.loadBinary()/bdos.cfg for why
		// this is a raw-binary load rather than the other two ROMs' Intel-hex path, and
		// why $8000-$80FF needs its own plain basicMemory (BDOS's writable working
		// state) alongside the $A000-$A7FF ROM (BDOS's actual code).
		bus.registerDevice(new basicMemory(bus, new AddressRange(0x8000, 0x81ff), null, 0));
		basicROM bdos = new basicROM(bus, new AddressRange(0xa000, 0xb7ff), null, 0);
		bdos.loadBinary("bdos.bin");
		bus.registerDevice(bdos);

		VIA6522 via = new VIA6522(bus, new AddressRange(0x9000, 0x900f));
		bus.registerDevice(via);

		// Video, same as CPUMain - the sole console now. RESET_INIT calls
		// BIOS_VIDEO_INIT unconditionally on every boot, so this fixture needs it
		// registered even for tests that never look at Video's own output, or that
		// fault on unmapped memory before the monitor prompt ever comes up.
		basicMemory videoRam = new basicMemory(bus, new AddressRange(0xb800, 0xbfff), null, 0);
		bus.registerDevice(videoRam);
		basicMemory graphicsRam = new basicMemory(bus, new AddressRange(0xc000, 0xdfff), null, 0);
		bus.registerDevice(graphicsRam);
		video = new VideoController(new AddressRange(0x9020, 0x9024), videoRam, graphicsRam);
		bus.registerDevice(video);

		fdc = new FloppyController(() -> via.requestInterrupt(VIA6522.FLAG_CB1), new AddressRange(0x9010, 0x9014));
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

		video.type("D 2000\r");
		runBriefly();

		assertTrue(video.getRow(0).contains("D 2000"), "the typed command should be echoed");
		assertTrue(video.getRow(1).startsWith(
				"2000  48 49 00 00 00 00 00 00  00 00 00 00 00 00 00 00  |HI..............|"),
				"first dump line, got: [" + video.getRow(1) + "]");
		assertTrue(video.getRow(16).startsWith("20F0"), "16 lines of 16 bytes covers $2000-$20FF");
	}

	@Test
	public void writeStoresBytesStartingAtTheGivenAddress() throws Exception {
		video.type("W 2000 48 49 4a\r");
		runBriefly();

		assertEquals(0x48, c.bus.read(0x2000) & 0xff);
		assertEquals(0x49, c.bus.read(0x2001) & 0xff);
		assertEquals(0x4a, c.bus.read(0x2002) & 0xff);
	}

	@Test
	public void lineFeedAloneAlsoTerminatesTheLine() throws Exception {
		// Swing's keyTyped for Enter isn't consistently '\r' across platforms - a real
		// interactive run got stuck here waiting for a '\r' that never arrived.
		video.type("W 2000 48\n");
		runBriefly();

		assertEquals(0x48, c.bus.read(0x2000) & 0xff);
	}

	@Test
	public void dollarPrefixIsAcceptedOnAddressesAndBytes() throws Exception {
		video.type("W $2000 $48 $49\r");
		runBriefly();

		assertEquals(0x48, c.bus.read(0x2000) & 0xff, "a bare '$' shouldn't make the address default to 0");
		assertEquals(0x49, c.bus.read(0x2001) & 0xff);
	}

	@Test
	public void unmappedMemoryAccessFaultsAndRecoversToThePrompt() throws Exception {
		c.bus.write(0x2000, (byte) 'H');

		// $9100 has no device registered on this bus (BDOS now claims $A000-$A7FF,
		// which this probed before BDOS existed) - dumping it should fault partway
		// through the first line instead of taking down the whole emulated machine.
		video.type("D 9100\r");
		video.type("D 2000\r");
		runBriefly();

		assertTrue(video.getRow(1).contains("9100"), "the dump should start before faulting");
		assertTrue(video.getRow(1).contains("?"), "the fault handler's error marker");

		boolean recovered = false;
		for (int row = 0; row <= 18; row++) {
			if (video.getRow(row).startsWith("2000")) {
				recovered = true;
				break;
			}
		}
		assertTrue(recovered, "the monitor should still be alive and process a command after the fault");
	}

	@Test
	public void backspaceErasesTheLastTypedCharacter() throws Exception {
		// Type a wrong byte value, backspace both digits away, then type the right one.
		video.type("W 2000 99\b\b48\r");
		runBriefly();

		assertEquals(0x48, c.bus.read(0x2000) & 0xff,
				"the erased '99' shouldn't have reached the parser at all");
	}

	@Test
	public void backspaceAtTheStartOfTheLineIsIgnored() throws Exception {
		video.type("\b\bD 2000\r");
		runBriefly();

		assertTrue(video.getRow(1).startsWith("2000"),
				"leading backspaces with nothing to erase shouldn't break the command");
	}

	@Test
	public void helpListsTheAvailableCommands() throws Exception {
		video.type("H\r");
		runBriefly();

		assertTrue(video.getRow(1).contains("D addr") && video.getRow(1).contains("dump"),
				"got: [" + video.getRow(1) + "]");
		assertTrue(video.getRow(2).contains("W addr") && video.getRow(2).contains("write"),
				"got: [" + video.getRow(2) + "]");
		assertTrue(video.getRow(3).contains("H") && video.getRow(3).contains("help"),
				"got: [" + video.getRow(3) + "]");
		assertTrue(video.getRow(5).contains("S drive") && video.getRow(5).contains("select"),
				"got: [" + video.getRow(5) + "]");
		assertTrue(video.getRow(6).contains("F [fill]") && video.getRow(6).contains("format"),
				"got: [" + video.getRow(6) + "]");
		assertTrue(video.getRow(7).contains("L track") && video.getRow(7).contains("load"),
				"got: [" + video.getRow(7) + "]");
		assertTrue(video.getRow(8).contains("P track") && video.getRow(8).contains("write"),
				"got: [" + video.getRow(8) + "]");
		assertTrue(video.getRow(9).contains("B") && video.getRow(9).contains("boot"),
				"got: [" + video.getRow(9) + "]");
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

		video.type("E 3000\r");
		video.type("D 2000\r");
		runBriefly();

		assertEquals(0x99, c.bus.read(0x2000) & 0xff, "the executed routine should have run");

		boolean recovered = false;
		for (int row = 0; row <= 18; row++) {
			if (video.getRow(row).startsWith("2000")) {
				recovered = true;
				break;
			}
		}
		assertTrue(recovered, "the monitor should still be alive after E's routine returns via RTS");
	}

	@Test
	public void formatFillsEveryTrackOfTheSelectedDriveWithTheGivenByte() throws Exception {
		video.type("S 0\r");
		video.type("F AA\r");
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
	public void formatWithNoArgumentDefaultsToE5() throws Exception {
		video.type("S 0\r");
		video.type("F\r");
		runBriefly();

		for (byte b : drive0.read()) {
			assertEquals((byte) 0xE5, b, "no fill byte given should default to CP/M's STATUS_EMPTY ($E5)");
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

		video.type("S 0\r");
		video.type("L 0A 05 3000\r");
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

		video.type("S 0\r");
		video.type("P 0B 06 4000\r");
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

		video.type("S 1\r");
		video.type("P 02 03 5000\r");
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
	public void bootFindsAndLoadsCcpComFromTheFilesystem() throws Exception {
		// B no longer reads track 0 at all - it asks BDOS to find a file named
		// CCP.COM and load its content, same as real CP/M's cold loader. The
		// content here is the same tiny LDA #$99 / STA $2000 / RTS subroutine the
		// E test uses, written through the same CpmFileSystem (modules/Disk) that
		// validates this on-disk format, standing in for what CpmDiskTool's "put"
		// subcommand would do with a real cl65-built CCP.COM.
		byte[] program = {
				(byte) 0xA9, (byte) 0x99,	// LDA #$99
				(byte) 0x8D, (byte) 0x00, (byte) 0x20,	// STA $2000
				(byte) 0x60	// RTS
		};
		new CpmFileSystem(drive0).format();
		new CpmFileSystem(drive0).createFile("CCP.COM", program);

		video.type("B\r");
		video.type("D 2000\r");
		runBriefly();

		assertEquals(0x99, c.bus.read(0x2000) & 0xff, "the booted routine should have run");

		boolean recovered = false;
		for (int row = 0; row <= 18; row++) {
			if (video.getRow(row).startsWith("2000")) {
				recovered = true;
				break;
			}
		}
		assertTrue(recovered, "the monitor should still be alive after the boot routine returns via RTS");
	}

	@Test
	public void bootPrintsQuestionMarkAndRecoversWhenCcpComIsMissing() throws Exception {
		new CpmFileSystem(drive0).format(); // formatted, but no CCP.COM written

		video.type("B\r");
		video.type("D 2000\r");
		runBriefly();

		assertTrue(video.getRow(1).startsWith("?"), "got: [" + video.getRow(1) + "]");

		boolean recovered = false;
		for (int row = 0; row <= 18; row++) {
			if (video.getRow(row).startsWith("2000")) {
				recovered = true;
				break;
			}
		}
		assertTrue(recovered, "the monitor should still be alive after a failed boot");
	}

	@Test
	public void bootedProgramCanPrintThroughTheBiosPutchPrimitive() throws Exception {
		// LDA #char / JSR BIOS_PUTCH ($F000), repeated per character, then RTS - a
		// boot image that actually calls back into the primitives ROM rather than
		// just poking memory directly, proving out the whole B -> BDOS_FIND_FILE/
		// BDOS_READ_RECORD -> JSR BOOTADDR -> BIOS_PUTCH round trip with a real (if
		// tiny) loaded program.
		byte[] program = {
				(byte) 0xA9, 0x48, 0x20, 0x00, (byte) 0xF0,	// LDA #'H' / JSR BIOS_PUTCH
				(byte) 0xA9, 0x65, 0x20, 0x00, (byte) 0xF0,	// LDA #'e' / JSR BIOS_PUTCH
				(byte) 0xA9, 0x6C, 0x20, 0x00, (byte) 0xF0,	// LDA #'l' / JSR BIOS_PUTCH
				(byte) 0xA9, 0x6C, 0x20, 0x00, (byte) 0xF0,	// LDA #'l' / JSR BIOS_PUTCH
				(byte) 0xA9, 0x6F, 0x20, 0x00, (byte) 0xF0,	// LDA #'o' / JSR BIOS_PUTCH
				0x60							// RTS
		};
		new CpmFileSystem(drive0).format();
		new CpmFileSystem(drive0).createFile("CCP.COM", program);

		video.type("B\r");
		runBriefly();

		assertTrue(video.getRow(1).contains("Hello"), "got: [" + video.getRow(1) + "]");
	}

	@Test
	public void printingPastTheBottomScrollsRepeatedlyWithoutFaulting() throws Exception {
		// Regression test: PC_LF's bulk add to V_ADDR_LO/HI (BIOS.asm) had no
		// ring-wrap check of its own, unlike PC_ADVANCE's single-byte increment -
		// it eventually walked V_ADDR_HI straight past VIDEO_RAM+VIDEO_RAM_SIZE
		// into genuinely unmapped memory once the char RAM ring had wrapped at
		// least once, faulting with a DeviceUnavailable. Caught interactively via
		// CCP's TYPE command on a real bare-LF file - edit.c's own redraws always
		// re-seed V_ADDR_LO/HI via BIOS_GOTOXY first, so edit.c's tests never
		// exercised PC_LF's own address math far enough to hit this. LDX #60
		// prints far more lines than one V_ROWS=24 screen holds, forcing several
		// scrolls - enough to wrap the 2048-byte ring at least once. The trailing
		// 'Z' only prints if the whole loop returned without faulting partway
		// through.
		byte[] program = {
				(byte) 0xA2, 0x3C,				// LDX #60
				(byte) 0xA9, 0x41,				// LDA #'A'
				0x20, 0x00, (byte) 0xF0,		// JSR BIOS_PUTCH
				(byte) 0xA9, 0x0A,				// LDA #$0A (LF)
				0x20, 0x00, (byte) 0xF0,		// JSR BIOS_PUTCH
				(byte) 0xCA,					// DEX
				(byte) 0xD0, (byte) 0xF3,		// BNE back to LDA #'A'
				(byte) 0xA9, 0x5A,				// LDA #'Z'
				0x20, 0x00, (byte) 0xF0,		// JSR BIOS_PUTCH
				0x60							// RTS
		};
		new CpmFileSystem(drive0).format();
		new CpmFileSystem(drive0).createFile("CCP.COM", program);

		video.type("B\r");
		runBriefly();

		boolean sawZ = false;
		for (int row = 0; row <= 23; row++) {
			if (video.getRow(row).contains("Z")) {
				sawZ = true;
				break;
			}
		}
		assertTrue(sawZ, "the loop should have run to completion and printed Z without faulting on an unmapped address");
	}

	@Test
	public void bootLoadsMultipleRecordsConsecutively() throws Exception {
		// A 256-byte CCP.COM: the first 128 bytes are NOPs ($EA) with no RTS/JMP at
		// all - the only way execution reaches the real routine in the second
		// 128-byte record is if BDOS_READ_RECORD's caller (DI_BOOT) advanced the
		// destination pointer by exactly 128 between the two BDOS_READ_RECORD
		// calls, landing the second record right after the first's last NOP falls
		// through.
		byte[] program = new byte[256];
		Arrays.fill(program, 0, 128, (byte) 0xEA); // NOP
		program[128] = (byte) 0xA9; // LDA #$99
		program[129] = (byte) 0x99;
		program[130] = (byte) 0x8D; // STA $2000
		program[131] = (byte) 0x00;
		program[132] = (byte) 0x20;
		program[133] = (byte) 0x60; // RTS
		new CpmFileSystem(drive0).format();
		new CpmFileSystem(drive0).createFile("CCP.COM", program);

		video.type("B\r");
		video.type("D 2000\r");
		runBriefly();

		assertEquals(0x99, c.bus.read(0x2000) & 0xff,
				"execution should fall through from the NOP-filled first sector into the second");
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

	@Test
	public void bootedProgramCanDrawAndClearPixelsThroughTheGraphicsPrimitives() throws Exception {
		// LDA #1 / JSR BIOS_GRAPHICS_MODE ($F01B), then a handful of
		// "stage column into $01/$02, stage on/off into X, row in A, JSR
		// BIOS_SET_PIXEL ($F01E)" sequences - see BIOS.asm's DO_SET_PIXEL for the
		// calling convention. Exercises: (0,0) and (7,0), the two ends of the
		// same byte's bit range (mod-8 boundary); (319,199), the bottom-right
		// corner of the 320x200 bitmap (column needs both staging bytes, since
		// 319 > 255); and (3,2) set then cleared, to prove the AND-mask path
		// actually clears a bit rather than just never setting it.
		byte[] program = {
				(byte) 0xA9, 0x01, 0x20, 0x1B, (byte) 0xF0,			// LDA #1 / JSR BIOS_GRAPHICS_MODE

				(byte) 0xA9, 0x00, (byte) 0x85, 0x01,				// LDA #0 / STA $01 (col lo)
				(byte) 0xA9, 0x00, (byte) 0x85, 0x02,				// LDA #0 / STA $02 (col hi)
				(byte) 0xA2, 0x01,						// LDX #1 (on)
				(byte) 0xA9, 0x00,						// LDA #0 (row)
				0x20, 0x1E, (byte) 0xF0,					// JSR BIOS_SET_PIXEL -> (0,0) on

				(byte) 0xA9, 0x07, (byte) 0x85, 0x01,				// col lo = 7
				(byte) 0xA9, 0x00, (byte) 0x85, 0x02,				// col hi = 0
				(byte) 0xA2, 0x01,
				(byte) 0xA9, 0x00,
				0x20, 0x1E, (byte) 0xF0,					// -> (7,0) on

				(byte) 0xA9, 0x3F, (byte) 0x85, 0x01,				// col lo = 0x3F
				(byte) 0xA9, 0x01, (byte) 0x85, 0x02,				// col hi = 1  (319)
				(byte) 0xA2, 0x01,
				(byte) 0xA9, (byte) 0xC7,					// row = 199
				0x20, 0x1E, (byte) 0xF0,					// -> (319,199) on

				(byte) 0xA9, 0x03, (byte) 0x85, 0x01,				// col lo = 3
				(byte) 0xA9, 0x00, (byte) 0x85, 0x02,				// col hi = 0
				(byte) 0xA2, 0x01,
				(byte) 0xA9, 0x02,						// row = 2
				0x20, 0x1E, (byte) 0xF0,					// -> (3,2) on

				(byte) 0xA9, 0x03, (byte) 0x85, 0x01,				// same (3,2) again,
				(byte) 0xA9, 0x00, (byte) 0x85, 0x02,
				(byte) 0xA2, 0x00,						// but X = 0 (off) this time
				(byte) 0xA9, 0x02,
				0x20, 0x1E, (byte) 0xF0,					// -> (3,2) off

				0x60								// RTS
		};
		new CpmFileSystem(drive0).format();
		new CpmFileSystem(drive0).createFile("CCP.COM", program);

		video.type("B\r");
		runBriefly();

		assertTrue(video.isGraphicsMode(), "BIOS_GRAPHICS_MODE should have switched VideoController into graphics mode");
		assertTrue(video.getPixel(0, 0), "(0,0) should be on");
		assertTrue(video.getPixel(7, 0), "(7,0), the other end of the same byte, should be on");
		for (int x = 1; x < 7; x++) {
			assertFalse(video.getPixel(x, 0), "x=" + x + " of row 0's first byte should be untouched");
		}
		assertTrue(video.getPixel(319, 199), "(319,199), the bottom-right corner, should be on");
		assertFalse(video.getPixel(3, 2), "(3,2) was set and then cleared - should be off again");
	}
}
