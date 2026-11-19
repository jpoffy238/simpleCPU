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
import com.mj.IntelHex.IntelHexLoader;
import com.mj.z80.instructions.FullInstructionSet;

/** Proves BDOS_RUN_FILE (bdos.c's load_and_run()) works end to end on
 * Z80 - the one BDOS primitive left deliberately untested when BDOS/CCP
 * were first ported, since SDCC's own default crt0 (reset-vector-at-0,
 * see Test_Z80HelloWorld's own comment) isn't something BDOS's "load
 * record by record to a fixed BOOTADDR and jump straight in" mechanism
 * can work with - ccp.ihx now links against cpm_crt0.asm instead (see
 * the Makefile's own comment) specifically to make this possible.
 *
 * Boots a tiny hand-written bootstrap (RunFileTest.asm) that calls
 * BDOS_RUN_FILE directly to load a real "CCP.COM" - seeded on the test's
 * own disk from the real, SDCC-built ccp.ihx's own bytes, flattened via
 * IntelHexLoader.flatten() (also what CpmDiskTool's own put() command
 * uses now, so a real disk image can be seeded by hand the same way) -
 * and confirms CCP's own real "A>" prompt appears, proving the whole
 * find-file/load-record-by-record/jump-to-BOOTADDR chain, and
 * cpm_crt0.asm's own minimal entry, all work together correctly. */
public class Test_Z80BdosRunFile {
	@TempDir
	Path tempDir;

	private Z80VideoDiskMachine machine;
	private Z80CPU cpu;

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
		CpmFileSystem fs = new CpmFileSystem(drive0);
		fs.format();
		fs.createFile("CCP.COM", IntelHexLoader.flatten(ccpIhx().toString()));

		machine.attachDrive(0, diskPath);
		machine.loadIntoMemory("BIOS.z80");
		machine.loadIntoMemory("RunFileTest.z80");
		machine.loadIntelHexIntoMemory(bdosIhx().toString(), 0);

		Z80InstructionDecoder decoder = new Z80InstructionDecoder();
		FullInstructionSet.populate(decoder);
		cpu = new Z80CPU(machine.memoryBus(), machine.portBus(), decoder);
		cpu.pc = Z80BinaryFile.read("RunFileTest.z80").startAddress;
	}

	@Test
	public void bdosRunFileLoadsAndJumpsIntoARealProgramFromDisk() throws Exception {
		for (int i = 0; i < 2_000_000; i++) {
			cpu.serviceInterrupts();
			cpu.step();
		}

		String row0 = machine.video().getRow(0);
		assertTrue(row0.startsWith("A>"), "expected CCP's own prompt after BDOS_RUN_FILE loaded and jumped into it - got: ["
				+ row0 + "]");
	}
}
