package com.mj.z80;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.mj.Disk.CpmFileSystem;
import com.mj.Disk.EightInch.IBM3740;
import com.mj.IntelHex.IntelHexLoader;
import com.mj.Video.VideoController;
import com.mj.z80.instructions.FullInstructionSet;

/** Drives the real, SDCC-compiled edit.ihx (shared edit.c with the 6502
 * side - see modules/cpu/c-shared) through a real Z80VideoDiskMachine,
 * same fixture shape as Test_Z80CCP - proving the full-screen editor
 * works correctly through real BIOS/BDOS calls on Z80, not just that it
 * compiles.
 *
 * Two boot styles, same reasoning as the 6502 side's own Test_Edit:
 * bootEditDirectly() loads edit.ihx straight onto the bus and jumps in,
 * isolating edit.c's own logic without a shell in between - simpler than
 * the 6502 side's own "disguise edit.bin as CCP.COM" trick, since Java
 * can just load any program directly at any address here, no monitor
 * boot command needed. The save/reopen test instead boots the real
 * ccp.ihx and launches EDIT by typing its name - the only way to
 * exercise BDOS_RUN_FILE's own warm-boot behavior (reload CCP.COM fresh
 * once the launched program returns), proving BDOS_RUN_FILE generalizes
 * beyond CCP reloading itself (Test_Z80BdosRunFile's own proof) to
 * launching a genuinely different program by name.
 *
 * bdos.ihx/edit.ihx/ccp.ihx aren't part of the Maven build (manual step,
 * same convention as the 6502 side's own edit.bin/ccp.bin) - every test
 * here skips rather than fails if they haven't been built yet. */
public class Test_Z80Edit {
	private static final String CTRL_O = "\u000F";
	private static final String CTRL_S = "\u0013";
	private static final String CTRL_X = "\u0018";

	@TempDir
	Path tempDir;

	private Z80VideoDiskMachine machine;
	private Z80CPU cpu;
	private CpmFileSystem fs;
	private String diskPath;

	private static Path bdosIhx() {
		return Paths.get("src/main/c/bdos.ihx");
	}

	private static Path editIhx() {
		return Paths.get("src/main/c/edit.ihx");
	}

	private static Path ccpIhx() {
		return Paths.get("src/main/c/ccp.ihx");
	}

	@BeforeEach
	public void setup() throws Exception {
		Assumptions.assumeTrue(Files.exists(bdosIhx()),
				"bdos.ihx not built - run `make` in modules/cpu/z80/src/main/c first");
		Assumptions.assumeTrue(Files.exists(editIhx()),
				"edit.ihx not built - run `make` in modules/cpu/z80/src/main/c first");

		machine = new Z80VideoDiskMachine();
		diskPath = tempDir.resolve("z80-disk0.img").toString();
		IBM3740 drive0 = new IBM3740();
		drive0.setVirtualDiskFile(diskPath);
		fs = new CpmFileSystem(drive0);
		fs.format();

		machine.attachDrive(0, diskPath);
		machine.loadIntoMemory("BIOS.z80");
		machine.loadIntelHexIntoMemory(bdosIhx().toString(), 0);
	}

	/** edit.ihx loaded directly onto the bus and jumped into - isolates
	 * edit.c's own logic, no shell in between. */
	private void bootEditDirectly() throws Exception {
		machine.loadIntelHexIntoMemory(editIhx().toString(), 0);

		Z80InstructionDecoder decoder = new Z80InstructionDecoder();
		FullInstructionSet.populate(decoder);
		cpu = new Z80CPU(machine.memoryBus(), machine.portBus(), decoder);
		cpu.pc = 0x1000;
	}

	/** The real ccp.ihx, with EDIT.COM seeded on disk and launched by
	 * typing its name at the CCP prompt - the only way to exercise
	 * BDOS_RUN_FILE's warm boot, since that's CCP's own behavior for a
	 * program it launched by name, not anything edit.c does itself.
	 *
	 * CCP.COM must exist as a real file here too, not just be loaded
	 * directly onto the bus for this first boot (a convenience the other
	 * tests' bootEditDirectly() uses, since they never exit back to a
	 * shell) - a real bug found 2026-11-19: without it, EDIT's own exit
	 * triggers bdos_run_file()'s warm boot, which can't find "CCP.COM" on
	 * disk, falls into BDOS's own "nothing safe to do" fallback
	 * (bios_getch() forever, silently discarding every further keystroke
	 * with no visible effect) - every character past that point still
	 * gets "consumed" from VideoController's queue, just by the wrong
	 * loop, which made the symptom look like a timing/step-budget issue
	 * at first rather than a missing file. */
	private void bootRealCcpThenLaunchEdit() throws Exception {
		Assumptions.assumeTrue(Files.exists(ccpIhx()),
				"ccp.ihx not built - run `make` in modules/cpu/z80/src/main/c first");
		fs.createFile("CCP.COM", IntelHexLoader.flatten(ccpIhx().toString()));
		fs.createFile("EDIT.COM", IntelHexLoader.flatten(editIhx().toString()));
		machine.loadIntelHexIntoMemory(ccpIhx().toString(), 0);

		Z80InstructionDecoder decoder = new Z80InstructionDecoder();
		FullInstructionSet.populate(decoder);
		cpu = new Z80CPU(machine.memoryBus(), machine.portBus(), decoder);
		cpu.pc = 0x1000;
	}

	private void runSteps(int steps) throws Exception {
		for (int i = 0; i < steps; i++) {
			cpu.serviceInterrupts();
			cpu.step();
		}
	}

	@Test
	public void typingShowsUpOnTheCorrectViewportRowAndStatusLine() throws Exception {
		bootEditDirectly();
		machine.video().type("HELLO\rWORLD");
		runSteps(5_000_000);

		assertTrue(machine.video().getRow(0).startsWith("^O Open"), "title bar, got: [" + machine.video().getRow(0) + "]");
		assertTrue(machine.video().getRow(1).startsWith("HELLO"), "row 1, got: [" + machine.video().getRow(1) + "]");
		assertTrue(machine.video().getRow(2).startsWith("WORLD"), "row 2, got: [" + machine.video().getRow(2) + "]");
		assertTrue(machine.video().getRow(23).contains("L:2") && machine.video().getRow(23).contains("C:6"),
				"status line, got: [" + machine.video().getRow(23) + "]");
	}

	@Test
	public void arrowKeysMoveTheCursorWithoutDisturbingViewportContent() throws Exception {
		bootEditDirectly();
		machine.video().type("HELLO");
		machine.video().pressArrowKey(VideoController.KEY_LEFT);
		machine.video().pressArrowKey(VideoController.KEY_LEFT);
		runSteps(5_000_000);

		assertEquals("HELLO" + " ".repeat(75), machine.video().getRow(1),
				"a pure cursor move (no buffer edit) shouldn't have disturbed the viewport row, got: ["
						+ machine.video().getRow(1) + "]");
		assertTrue(machine.video().getRow(23).contains("C:4"), "status line, got: [" + machine.video().getRow(23) + "]");
	}

	@Test
	public void saveThenReopenRoundTripsTheBufferThroughAWarmBoot() throws Exception {
		bootRealCcpThenLaunchEdit();
		machine.video().type("EDIT\r");
		machine.video().type("HELLO\rWORLD");
		machine.video().type(CTRL_S);
		machine.video().type("TEST.TXT\r");
		machine.video().type(CTRL_X);
		machine.video().type("EDIT\r");
		machine.video().type(CTRL_O);
		machine.video().type("TEST.TXT\r");
		runSteps(5_000_000);

		assertTrue(machine.video().getRow(1).startsWith("HELLO"), "reloaded row 1, got: [" + machine.video().getRow(1) + "]");
		assertTrue(machine.video().getRow(2).startsWith("WORLD"), "reloaded row 2, got: [" + machine.video().getRow(2) + "]");
		assertTrue(machine.video().getRow(23).contains("Loaded"), "status line, got: [" + machine.video().getRow(23) + "]");

		byte[] onDisk = fs.readFile("TEST.TXT");
		String content = new String(onDisk, "US-ASCII");
		assertTrue(content.startsWith("HELLO\rWORLD\r"),
				"saved file content, got: [" + content.replace("\r", "\\r") + "]");
	}
}
