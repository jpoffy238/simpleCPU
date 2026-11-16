package com.mj.Firmware.FileDevice;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

/** Drives the real, cl65-compiled edit.bin through a real CPU + Video, same
 * fixture shape as Test_BIOS. Most tests boot it directly AS "CCP.COM" (same
 * trick Test_BIOS's own bootFindsAndLoadsCcpComFromTheFilesystem uses) to
 * exercise edit.c in isolation, without a real shell layer in between - see
 * bootEditDirectly(). The one test needing CCP.COM's own warm-boot behavior
 * uses bootRealCcpThenLaunchEdit() instead, with the real ccp.bin in the mix
 * too.
 *
 * edit.bin (and, for one test, ccp.bin) aren't part of the Maven build -
 * they're produced by cc65 via `make` in src/main/c/ (see that Makefile) -
 * so every test here skips rather than fails if they haven't been built
 * yet, same accommodation Test_DormannFunctional makes for its own external
 * .bin dependency. */
public class Test_Edit {
	/* edit.c's own Ctrl+O/S/X bindings (see edit.c) - raw control-code
	 * values, injectable through VideoController.type()'s char-by-char
	 * loop same as any other character. */
	private static final String CTRL_O = "";
	private static final String CTRL_S = "";
	private static final String CTRL_X = "";

	@TempDir
	Path tempDir;

	private PBus bus;
	private VideoController video;
	private IBM3740 drive0;
	private CPU c;

	private static Path editBin() {
		return Paths.get("src/main/c/edit.bin");
	}

	private static Path ccpBin() {
		return Paths.get("src/main/c/ccp.bin");
	}

	@BeforeEach
	public void setup() throws Exception {
		Assumptions.assumeTrue(Files.exists(editBin()),
				"edit.bin not built - run `make` in modules/cpu/6502/src/main/c first");

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
		video = new VideoController(new AddressRange(0x9020, 0x9023), videoRam);
		bus.registerDevice(video);

		FloppyController fdc = new FloppyController(via, new AddressRange(0x9010, 0x9014));
		bus.registerDevice(fdc);
		drive0 = new IBM3740();
		drive0.setVirtualDiskFile(tempDir.resolve("drive0.img").toString());
		fdc.attachDrive(0, drive0);
		IBM3740 drive1 = new IBM3740();
		drive1.setVirtualDiskFile(tempDir.resolve("drive1.img").toString());
		fdc.attachDrive(1, drive1);

		c = new CPU(bus, new cpu001decoder());
	}

	/** edit.bin loaded directly AS "CCP.COM" - MONITOR's B command does a
	 * one-time load-and-jump with no warm-boot-on-return behavior (that's
	 * specific to bdos_run_file(), triggered only by typing a program's
	 * name at a real CCP prompt - see bdos.c). Fine for tests that never
	 * exit edit.c and expect anything useful to happen afterward. */
	private void bootEditDirectly() throws Exception {
		byte[] editProgram = Files.readAllBytes(editBin());
		new CpmFileSystem(drive0).format();
		new CpmFileSystem(drive0).createFile("CCP.COM", editProgram);
		video.type("B\r");
	}

	/** The real CCP.COM plus edit.bin as a separate EDIT.COM, launched by
	 * typing its name at the CCP prompt - the only way to exercise
	 * bdos_run_file()'s warm boot (reload CCP.COM fresh once the launched
	 * program returns), since that's CCP.COM's own behavior, not
	 * anything edit.c does itself. */
	private void bootRealCcpThenLaunchEdit() throws Exception {
		Assumptions.assumeTrue(Files.exists(ccpBin()),
				"ccp.bin not built - run `make` in modules/cpu/6502/src/main/c first");
		byte[] ccpProgram = Files.readAllBytes(ccpBin());
		byte[] editProgram = Files.readAllBytes(editBin());
		CpmFileSystem fs = new CpmFileSystem(drive0);
		fs.format();
		fs.createFile("CCP.COM", ccpProgram);
		fs.createFile("EDIT.COM", editProgram);
		video.type("B\r");
		video.type("EDIT\r");
	}

	/** Longer than Test_BIOS's own 2s - every test here runs a real program,
	 * not just one monitor command. */
	private void runBriefly() throws Exception {
		runBriefly(3000);
	}

	/** For saveThenReopenRoundTripsTheBufferThroughAWarmBoot specifically -
	 * two full CCP.COM boots plus two bdos_run_file() loads is meaningfully
	 * more real work than any other test here, so it gets more headroom
	 * rather than sharing the default budget above. */
	private void runBriefly(long millis) throws Exception {
		c.start();
		Thread.sleep(millis);
		c.interrupt();
	}

	@Test
	public void typingShowsUpOnTheCorrectViewportRowAndStatusLine() throws Exception {
		bootEditDirectly();
		video.type("HELLO\rWORLD");
		runBriefly();

		assertTrue(video.getRow(0).startsWith("^O Open"), "title bar, got: [" + video.getRow(0) + "]");
		assertTrue(video.getRow(1).startsWith("HELLO"), "row 1, got: [" + video.getRow(1) + "]");
		assertTrue(video.getRow(2).startsWith("WORLD"), "row 2, got: [" + video.getRow(2) + "]");
		assertEquals("HELLO" + " ".repeat(75), video.getRow(1),
				"row 1 should be exactly HELLO plus trailing blanks, got: [" + video.getRow(1) + "]");
		assertTrue(video.getRow(23).contains("L:2") && video.getRow(23).contains("C:6"),
				"status line, got: [" + video.getRow(23) + "]");
	}

	@Test
	public void arrowKeysMoveTheCursorWithoutDisturbingViewportContent() throws Exception {
		// CPU extends Thread, so runBriefly() (which calls c.start()) can only
		// ever run once per test - every keystroke has to be queued upfront,
		// not staggered across multiple start()/interrupt() cycles. Asserting
		// the row is still exactly "HELLO" after the moves proves the same
		// thing a separate before/after snapshot would have, without needing
		// a second run.
		bootEditDirectly();
		video.type("HELLO");
		video.pressArrowKey(VideoController.KEY_LEFT);
		video.pressArrowKey(VideoController.KEY_LEFT);
		runBriefly();

		assertEquals("HELLO" + " ".repeat(75), video.getRow(1),
				"a pure cursor move (no buffer edit) shouldn't have disturbed the viewport row, got: ["
						+ video.getRow(1) + "]");
		assertTrue(video.getRow(23).contains("C:4"), "status line, got: [" + video.getRow(23) + "]");
	}

	@Test
	public void backspaceJoiningTwoLinesRedrawsBelowCorrectly() throws Exception {
		bootEditDirectly();
		// HELLO / WORLD on two lines, then move to the start of WORLD and
		// backspace - joins them into one "HELLOWORLD" line, shifting
		// everything below up by one (the cross-line-join path).
		video.type("HELLO\rWORLD");
		video.pressArrowKey(VideoController.KEY_LEFT);
		video.pressArrowKey(VideoController.KEY_LEFT);
		video.pressArrowKey(VideoController.KEY_LEFT);
		video.pressArrowKey(VideoController.KEY_LEFT);
		video.pressArrowKey(VideoController.KEY_LEFT);
		video.type("\b");
		runBriefly();

		assertTrue(video.getRow(1).startsWith("HELLOWORLD"), "got: [" + video.getRow(1) + "]");
		assertEquals(" ".repeat(80), video.getRow(2),
				"the old second row should be blank now that everything merged into row 1, got: ["
						+ video.getRow(2) + "]");
	}

	@Test
	public void saveThenReopenRoundTripsTheBufferThroughAWarmBoot() throws Exception {
		// Needs the REAL CCP.COM in the loop, unlike the other tests here -
		// bdos_run_file()'s warm boot (reload CCP.COM fresh once the
		// launched program returns) is CCP.COM's own behavior for a
		// program it launched by name, not anything edit.c does itself.
		// Booting edit.bin directly via MONITOR's B (what bootEditDirectly()
		// does) has no such behavior - its own return just falls back to
		// the bare MONITOR, which found an earlier draft of this test
		// faulting on these same keystrokes once they hit MONITOR's command
		// parser instead of a second edit.c instance. This is the round trip
		// the owner already confirmed by hand interactively, now exercising
		// the same CCP "type EDIT to run it" path that confirmation used.
		bootRealCcpThenLaunchEdit();
		video.type("HELLO\rWORLD");
		video.type(CTRL_S);
		video.type("TEST.TXT\r");
		video.type(CTRL_X);
		video.type("EDIT\r");
		video.type(CTRL_O);
		video.type("TEST.TXT\r");
		runBriefly(5000);

		assertTrue(video.getRow(1).startsWith("HELLO"), "reloaded row 1, got: [" + video.getRow(1) + "]");
		assertTrue(video.getRow(2).startsWith("WORLD"), "reloaded row 2, got: [" + video.getRow(2) + "]");
		// "Loaded", not the filename/position line - status_msg()'s own
		// message is still showing, since nothing typed afterward has
		// triggered the next draw_status() that would replace it (see
		// edit.c's status_pending comment).
		assertTrue(video.getRow(23).contains("Loaded"), "status line, got: [" + video.getRow(23) + "]");

		byte[] onDisk = new CpmFileSystem(drive0).readFile("TEST.TXT");
		String content = new String(onDisk, "US-ASCII");
		assertTrue(content.startsWith("HELLO\rWORLD\r"),
				"saved file content, got: [" + content.replace("\r", "\\r") + "]");
	}

	@Test
	public void openingAMissingFileReportsNotFoundAndLeavesTheBufferAlone() throws Exception {
		bootEditDirectly();
		video.type("HELLO");
		video.type(CTRL_O);
		video.type("NOSUCH.TXT\r");
		runBriefly();

		assertTrue(video.getRow(23).contains("Not found"), "got: [" + video.getRow(23) + "]");
		assertTrue(video.getRow(1).startsWith("HELLO"),
				"a failed Open shouldn't have touched the existing buffer, got: [" + video.getRow(1) + "]");
	}

	/** Builds a synthetic file with `lineCount` lines ("LINE0001", "LINE0002",
	 * ...), joined with bare Unix LF (not CR) - exercises load_file()'s
	 * universal-newline handling at the same time as paging, same way a
	 * real file authored on this Linux host (edit.c's own source, say,
	 * which is exactly what prompted this feature) would arrive. */
	private static byte[] syntheticLines(int lineCount) {
		StringBuilder sb = new StringBuilder();
		for (int i = 1; i <= lineCount; i++) {
			sb.append(String.format("LINE%04d\n", i));
		}
		return sb.toString().getBytes(java.nio.charset.StandardCharsets.US_ASCII);
	}

	@Test
	public void aFileBiggerThanOnePageSwapsPagesCorrectlyAndRoundTripsThroughSave() throws Exception {
		// 220 lines = exactly 2 pages at PAGE_LINES=200 (200 + 20) - deliberately
		// not an exact multiple of 200, so the last page has real content of
		// its own rather than landing exactly on a page boundary (a separate,
		// rarer edge case not what this test is after). Bare LF line endings
		// exercise the same universal-newline fix the single-page tests above
		// don't need to (their HELLO\rWORLD already uses CR).
		byte[] editProgram = Files.readAllBytes(editBin());
		CpmFileSystem fs = new CpmFileSystem(drive0);
		fs.format();
		fs.createFile("CCP.COM", editProgram);
		fs.createFile("BIG.TXT", syntheticLines(220));

		video.type("B\r");
		video.type(CTRL_O);
		video.type("BIG.TXT\r");
		video.pressArrowKey(VideoController.KEY_PAGE_DOWN);
		video.pressArrowKey(VideoController.KEY_PAGE_UP);
		// Ctrl+S alone, no filename typed after: Open already set has_name,
		// so cmd_save() saves straight back to BIG.TXT without prompting -
		// it only prompts when there's no name yet (see the single-page
		// save-as-new-file test above). Typing a filename here anyway would
		// land as literal characters inserted into the document itself,
		// since there'd be no prompt open to consume them.
		video.type(CTRL_S);
		runBriefly(10000);

		assertTrue(video.getRow(1).startsWith("LINE0001"),
				"should be back on page 1 after a page-down then page-up, got: [" + video.getRow(1) + "]");
		assertTrue(video.getRow(23).contains("Saved"), "status line, got: [" + video.getRow(23) + "]");

		byte[] onDisk = new CpmFileSystem(drive0).readFile("BIG.TXT");
		String content = new String(onDisk, "US-ASCII");
		String[] savedLines = content.split("\r", -1);
		for (int i = 1; i <= 220; i++) {
			assertEquals(String.format("LINE%04d", i), savedLines[i - 1],
					"line " + i + " of the saved round trip, got: [" + savedLines[i - 1] + "]");
		}
	}

	@Test
	public void insertingIntoAFullPageWithoutMovingToItsLastLineGrowsANewPage() throws Exception {
		// Exactly 200 lines - PAGE_LINES - loads as one clean, completely full page
		// now that load_file()'s own end-of-file-on-a-boundary off-by-one is fixed
		// (it used to leave a phantom second page behind for a file shaped exactly
		// like this one). Pressing Enter immediately, right at line 1 (not the
		// page's own last line), exercises split_line()'s "the page's real last
		// line is what a normal insert would push out - save it, make room, grow a
		// new page for it" path - no navigation keystrokes needed to set up, unlike
		// the cursor-at-the-last-line variant below.
		byte[] editProgram = Files.readAllBytes(editBin());
		CpmFileSystem fs = new CpmFileSystem(drive0);
		fs.format();
		fs.createFile("CCP.COM", editProgram);
		fs.createFile("FULL.TXT", syntheticLines(200));

		video.type("B\r");
		video.type(CTRL_O);
		video.type("FULL.TXT\r");
		video.type("\r");
		video.type(CTRL_S);
		runBriefly(10000);

		// No "P:1/2" check here: status_pending latches "Saved" on screen for
		// exactly one extra redraw past the save, and nothing else is typed
		// afterward, so the editor just blocks on the next keystroke (which
		// never comes) before draw_status() ever gets a chance to show the
		// page indicator again - the content/round-trip checks below are
		// what actually confirm the second page is real.
		assertEquals(" ".repeat(80), video.getRow(1), "the split's first half is now an empty line, got: [" + video.getRow(1) + "]");
		assertTrue(video.getRow(2).startsWith("LINE0001"), "got: [" + video.getRow(2) + "]");
		assertTrue(video.getRow(23).contains("Saved"), "status line, got: [" + video.getRow(23) + "]");

		byte[] onDisk = new CpmFileSystem(drive0).readFile("FULL.TXT");
		String content = new String(onDisk, "US-ASCII");
		String[] savedLines = content.split("\r", -1);
		assertEquals("", savedLines[0], "the new empty first line, got: [" + savedLines[0] + "]");
		for (int i = 1; i <= 199; i++) {
			assertEquals(String.format("LINE%04d", i), savedLines[i],
					"line " + i + " of the saved round trip, got: [" + savedLines[i] + "]");
		}
		assertEquals("LINE0200", savedLines[200],
				"the page's own last line, pushed out onto the new page, got: [" + savedLines[200] + "]");
	}

	@Test
	public void typingPastTheEndOfAFullPageGrowsANewPageInsteadOfRefusing() throws Exception {
		// Same 200-line setup as above, but this time walks the cursor all the way
		// down to the page's own last line (PAGE_LINES - 1), and all the way right
		// to the end of it (there's no "End" key - KEY_RIGHT one column at a time
		// is the only way), before pressing Enter - exercising the OTHER half of
		// split_line()'s growth logic: there's no tail left to push out, so the
		// split's own second half becomes the new page directly, and the cursor
		// moves onto it rather than staying put. Column has to actually reach the
		// end of "LINE0200" (8) first, or splitting at column 0 would move that
		// whole line's text onto the new page instead of leaving a genuinely empty
		// line there for "NEW LINE" to land on cleanly. 199 KEY_DOWNs is slow
		// enough (each is a full main-loop iteration, not just a register update)
		// that this needs a much longer budget than any other test in this file -
		// confirmed empirically, not just estimated, after an earlier attempt at
		// 12s only got partway through them before being cut off.
		byte[] editProgram = Files.readAllBytes(editBin());
		CpmFileSystem fs = new CpmFileSystem(drive0);
		fs.format();
		fs.createFile("CCP.COM", editProgram);
		fs.createFile("FULL.TXT", syntheticLines(200));

		video.type("B\r");
		video.type(CTRL_O);
		video.type("FULL.TXT\r");
		for (int i = 0; i < 199; i++) {
			video.pressArrowKey(VideoController.KEY_DOWN);
		}
		for (int i = 0; i < 8; i++) {
			video.pressArrowKey(VideoController.KEY_RIGHT);
		}
		video.type("\rNEW LINE");
		video.type(CTRL_S);
		runBriefly(40000);

		// No "P:2/2" check here either - see the comment on the same point in
		// the test above; "Saved" latches the status line past any chance of
		// draw_status() showing the page indicator again.
		assertTrue(video.getRow(1).startsWith("NEW LINE"),
				"cursor should have landed on the new page's own line 1, got: [" + video.getRow(1) + "]");
		assertTrue(video.getRow(23).contains("Saved"), "status line, got: [" + video.getRow(23) + "]");

		byte[] onDisk = new CpmFileSystem(drive0).readFile("FULL.TXT");
		String content = new String(onDisk, "US-ASCII");
		String[] savedLines = content.split("\r", -1);
		for (int i = 1; i <= 200; i++) {
			assertEquals(String.format("LINE%04d", i), savedLines[i - 1],
					"line " + i + " of the saved round trip, got: [" + savedLines[i - 1] + "]");
		}
		assertEquals("NEW LINE", savedLines[200], "the newly typed 201st line, got: [" + savedLines[200] + "]");
	}
}
