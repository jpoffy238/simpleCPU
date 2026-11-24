package com.mj.z80;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.mj.Disk.CpmFileSystem;
import com.mj.Disk.EightInch.IBM3740;
import com.mj.Disk.Imd.CpmImageReader;
import com.mj.Disk.Imd.DiskParameterBlock;
import com.mj.Disk.Imd.ImdImage;
import com.mj.z80.instructions.FullInstructionSet;

/** Boots tiny, hand-assembled fixtures that use the REAL CP/M 2.2 BDOS
 * calling convention (CALL 5, function number in C) against the new
 * CpmCompat.asm dispatcher - not this project's own custom BIOS_* and
 * BDOS_* ABI that CCP.COM/EDIT.COM use.
 *
 * Phase 1: console functions (System Reset/Console Input/Console Output/
 * Print String/Get Console Status/Get Version), delegating into the
 * existing, already-proven BIOS_PUTCH/BIOS_GETCH primitives rather than
 * reimplementing console I/O.
 *
 * Phase 2: read-only disk functions (Select Disk/Open File/Read
 * Sequential/Get Current Disk/Set DMA), delegating into the existing
 * BDOS_FIND_FILE/BDOS_READ_RECORD - which already read the real on-disk
 * CP/M format real CP/M itself uses, already validated against real disk
 * images (see CpmImageReader's own class comment). The real TRS-80 Model
 * II CP/M 2.2 disk's own PHYSICAL geometry (double-sided isn't an issue
 * here, but its own track/sector layout still doesn't fit this project's
 * IBM3740 geometry directly) doesn't matter for this test - only the
 * PROGRAM's bytes need to be real and unmodified; the disk container
 * carrying them is this project's own already-validated CpmFileSystem
 * format, the same one CCP.COM/EDIT.COM already use. */
public class Test_CpmCompat {
	@TempDir
	Path tempDir;

	private Z80VideoDiskMachine machine;
	private Z80CPU cpu;

	private static Path bdosIhx() {
		return Paths.get("src/main/c/bdos.ihx");
	}

	private static Path cpm22Imd() {
		return Paths.get("../../../images/cpm/CPM22.IMD");
	}

	@BeforeEach
	public void setup() throws Exception {
		machine = new Z80VideoDiskMachine();
		machine.loadIntoMemory("BIOS.z80");

		// Real CP/M's own fixed low-memory BDOS entry vector - a real
		// cold-boot loader would poke this once at boot time; this test
		// harness does the same thing by hand, same spirit as every other
		// hand-poked test fixture in this project. $0000 (WBOOT) is left
		// alone - neither fixture below ever jumps there.
		machine.memoryBus().write(0x0005, (byte) 0xC3); // JP
		machine.memoryBus().write(0x0006, (byte) 0x00);
		machine.memoryBus().write(0x0007, (byte) 0xE0); // -> $E000 (BDOS_ENTRY)

		Z80InstructionDecoder decoder = new Z80InstructionDecoder();
		FullInstructionSet.populate(decoder);
		cpu = new Z80CPU(machine.memoryBus(), machine.portBus(), decoder);
	}

	private void bootFixture(String fixtureName) throws Exception {
		machine.loadIntoMemory("CpmCompat.z80");
		machine.loadIntoMemory(fixtureName);
		cpu.pc = Z80BinaryFile.read(fixtureName).startAddress;
	}

	/** Runs until the CPU settles into a fixture's own terminal `DONE: jp
	 * DONE` self-loop (three consecutive identical PC values) - same
	 * technique Test_Z80DiskPrimitives/Test_Z80GetTime already use. */
	private void runToCompletion(int maxSteps) throws Exception {
		int lastPc = -1;
		int repeatCount = 0;
		for (int i = 0; i < maxSteps; i++) {
			int pc = cpu.pc;
			if (pc == lastPc && ++repeatCount == 3) {
				return;
			}
			if (pc != lastPc) {
				repeatCount = 0;
			}
			lastPc = pc;
			cpu.serviceInterrupts();
			cpu.step();
		}
		throw new AssertionError("fixture never reached its own DONE loop within " + maxSteps + " steps");
	}

	@Test
	public void realCallFiveFunctionNinePrintsAStringThroughTheExistingBiosPutchPrimitive() throws Exception {
		bootFixture("CpmHello.z80");
		runToCompletion(10_000);

		assertEquals('H', machine.memoryBus().read(0xb800) & 0xff);
		assertEquals('E', machine.memoryBus().read(0xb801) & 0xff);
		assertEquals('L', machine.memoryBus().read(0xb802) & 0xff);
		assertEquals('L', machine.memoryBus().read(0xb803) & 0xff);
		assertEquals('O', machine.memoryBus().read(0xb804) & 0xff);
	}

	@Test
	public void realCallFiveOpenAndReadSequentialReturnARealFilesRealBytes() throws Exception {
		Assumptions.assumeTrue(Files.exists(bdosIhx()),
				"bdos.ihx not built - run `make` in modules/cpu/z80/src/main/c first");
		Assumptions.assumeTrue(Files.exists(cpm22Imd()),
				"CPM22.IMD not present - real vintage disk images are kept local-only, see images/cpm's own .gitignore");
		machine.loadIntelHexIntoMemory(bdosIhx().toString(), 0);

		// Extracts PAGE.COM's real bytes off the real TRS-80 Model II CP/M
		// 2.2 disk via the already-proven CpmImageReader, then writes them
		// onto a fresh, synthetic disk under the same name via this
		// project's own CpmFileSystem.
		ImdImage realDisk = ImdImage.read(cpm22Imd());
		CpmImageReader reader = new CpmImageReader(realDisk, DiskParameterBlock.trs80ModelIiCpm22());
		byte[] realBytes = reader.readFile("PAGE.COM");

		IBM3740 drive0 = new IBM3740();
		drive0.setVirtualDiskFile(tempDir.resolve("z80-disk0.img").toString());
		CpmFileSystem fs = new CpmFileSystem(drive0);
		fs.format();
		fs.createFile("PAGE.COM", realBytes);
		machine.floppyController().attachDrive(0, drive0);

		bootFixture("CpmReadFile.z80");
		runToCompletion(50_000);

		byte[] readBack = new byte[realBytes.length];
		for (int i = 0; i < readBack.length; i++) {
			readBack[i] = machine.memoryBus().read(0x2000 + i);
		}
		assertArrayEquals(realBytes, readBack);
	}
}
