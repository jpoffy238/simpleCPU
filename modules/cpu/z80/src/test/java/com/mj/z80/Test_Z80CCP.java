package com.mj.z80;

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
import com.mj.z80.instructions.FullInstructionSet;

/** Drives the real, SDCC-compiled ccp.ihx (shared ccp.c/bdos.c with the
 * 6502 side - see modules/cpu/c-shared) through a real Z80VideoDiskMachine
 * with a real BDOS.ihx and a real formatted disk, same fixture shape as
 * Test_CCP on the 6502 side - proving BDOS's filesystem primitives and
 * CCP's own command dispatch work correctly together through real BIOS
 * calls, not just that each compiles.
 *
 * No Z80 monitor exists yet (unlike the 6502 side's MONITOR.asm `B`
 * command), so this boots ccp.ihx directly rather than through any
 * BDOS_RUN_FILE/disk-based load - same simplification Test_Z80HelloWorld
 * already uses for a single program. bdos.ihx/ccp.ihx aren't part of the
 * Maven build (manual step, same convention as the 6502 side's own
 * ccp.bin) - every test here skips rather than fails if they haven't been
 * built yet. */
public class Test_Z80CCP {
	@TempDir
	Path tempDir;

	private Z80VideoDiskMachine machine;
	private Z80CPU cpu;
	private CpmFileSystem fs;

	private static Path bdosIhx() {
		return Paths.get("src/main/c/bdos.ihx");
	}

	private static Path ccpIhx() {
		return Paths.get("src/main/c/ccp.ihx");
	}

	@BeforeEach
	public void setup() throws Exception {
		Assumptions.assumeTrue(Files.exists(bdosIhx()),
				"bdos.ihx not built - run `make` in modules/cpu/z80/src/main/c first");
		Assumptions.assumeTrue(Files.exists(ccpIhx()),
				"ccp.ihx not built - run `make` in modules/cpu/z80/src/main/c first");

		machine = new Z80VideoDiskMachine();

		String diskPath = tempDir.resolve("z80-disk0.img").toString();
		IBM3740 drive0 = new IBM3740();
		drive0.setVirtualDiskFile(diskPath);
		fs = new CpmFileSystem(drive0);
		fs.format();

		machine.attachDrive(0, diskPath);
		machine.loadIntoMemory("BIOS.z80");
		machine.loadIntelHexIntoMemory(bdosIhx().toString(), 0);
		machine.loadIntelHexIntoMemory(ccpIhx().toString(), 0);

		Z80InstructionDecoder decoder = new Z80InstructionDecoder();
		FullInstructionSet.populate(decoder);
		cpu = new Z80CPU(machine.memoryBus(), machine.portBus(), decoder);
		// ccp.ihx's own crt0 entry - NOT --code-loc's address, see
		// Test_Z80HelloWorld's own comment on why.
		cpu.pc = 0x0000;
	}

	private void runSteps(int steps) throws Exception {
		for (int i = 0; i < steps; i++) {
			cpu.serviceInterrupts();
			cpu.step();
		}
	}

	@Test
	public void statReportsTotalUsedAndFreeBlocksMatchingTheHostSideReference() throws Exception {
		int totalBlocks = fs.totalBlockCount();
		int freeBlocks = fs.freeBlockCount();
		int usedBlocks = fs.usedBlockCount();

		// Queued immediately - VideoController's own keystroke queue doesn't
		// care whether BIOS_GETCH is polling yet, so there's no need to wait
		// for the prompt to actually appear first (same reasoning
		// Test_Z80HelloWorld's own echo test already relies on).
		machine.video().type("STAT\r");
		runSteps(500_000);

		String output = findRowContaining("k total");
		assertTrue(output != null, "STAT output not found on screen");
		assertTrue(output.contains(usedBlocks + "k used"), "got: [" + output + "]");
		assertTrue(output.contains(freeBlocks + "k free"), "got: [" + output + "]");
		assertTrue(output.contains(totalBlocks + "k total"), "got: [" + output + "]");
	}

	private String findRowContaining(String substring) throws Exception {
		for (int row = 0; row <= 23; row++) {
			String line = machine.video().getRow(row);
			if (line.contains(substring)) {
				return line;
			}
		}
		return null;
	}
}
