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

/** Boots tiny, hand-assembled fixtures - and, in Phase 3, a real,
 * unmodified CP/M .COM binary - that use the REAL CP/M 2.2 BDOS calling
 * convention (CALL 5, function number in C) against the new CpmCompat.asm
 * dispatcher - not this project's own custom BIOS_* and BDOS_* ABI that
 * CCP.COM/EDIT.COM use.
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
 * format, the same one CCP.COM/EDIT.COM already use.
 *
 * Phase 3: the actual payoff - DUMP.COM, the real TRS-80 disk's own
 * hex-dump utility, loaded unmodified and run for real. The two smallest
 * real .COM files on the disk (PAGE.COM/SYNCRO.COM, picked when this
 * feature was scoped) both turned out to be poor fits once actually run,
 * not just read: PAGE.COM calls real BDOS function 5 (List Output - a
 * printer, deliberately out of scope, and PAGE.COM's own name/behavior
 * suggests it IS a print-pagination utility, so there'd be nothing
 * observable even if function 5 were stubbed) and SYNCRO.COM never calls
 * BDOS at all within a generous step budget, settling into its own
 * internal loop - found by tracing both with a throwaway diagnostic
 * harness before writing this test, exactly the "small, bounded surprise"
 * this feature's own plan anticipated. DUMP.COM (640 bytes, the next
 * smallest) needs a real filename argument (its first BDOS call is Open
 * File with DE=$5C, the default FCB1 a real CCP would have already
 * parsed) - given one (SAMPLE.TXT, another real file on the same disk),
 * it runs correctly indefinitely, genuinely hex-dumping SAMPLE.TXT's real
 * content through real console output calls. */
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

	/** Runs exactly `steps` instructions with no exit condition - unlike
	 * runToCompletion(), for a real program (DUMP.COM) that never settles
	 * into a terminal self-loop within any reasonable budget while it's
	 * still correctly, actively dumping. */
	private void runSteps(int steps) throws Exception {
		for (int i = 0; i < steps; i++) {
			cpu.serviceInterrupts();
			cpu.step();
		}
	}

	/** Calls a BIOS primitive directly from Java by pushing a synthetic
	 * return address, the same effect a real CALL instruction has - used
	 * here only for BIOS_VIDEO_INIT, which a real CCP would have already
	 * called once at boot, before any program (including DUMP.COM) ever
	 * runs; there's no CCP in this test, so this stands in for it. */
	private void callBios(int address) throws Exception {
		int savedPc = cpu.pc;
		int savedSp = cpu.sp;
		cpu.sp = 0xfffe;
		machine.memoryBus().write(0xfffe, (byte) 0x00);
		machine.memoryBus().write(0xffff, (byte) 0x00); // return address $0000
		cpu.pc = address;
		for (int i = 0; i < 2000 && cpu.pc != 0x0000; i++) {
			cpu.step();
		}
		cpu.pc = savedPc;
		cpu.sp = savedSp;
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

	@Test
	public void aRealUnmodifiedCpmUtilityHexDumpsARealFileThroughTheDispatcher() throws Exception {
		Assumptions.assumeTrue(Files.exists(bdosIhx()),
				"bdos.ihx not built - run `make` in modules/cpu/z80/src/main/c first");
		Assumptions.assumeTrue(Files.exists(cpm22Imd()),
				"CPM22.IMD not present - real vintage disk images are kept local-only, see images/cpm's own .gitignore");
		machine.loadIntelHexIntoMemory(bdosIhx().toString(), 0);

		ImdImage realDisk = ImdImage.read(cpm22Imd());
		CpmImageReader reader = new CpmImageReader(realDisk, DiskParameterBlock.trs80ModelIiCpm22());
		byte[] dumpCom = reader.readFile("DUMP.COM");
		byte[] sampleTxt = reader.readFile("SAMPLE.TXT");

		IBM3740 drive0 = new IBM3740();
		drive0.setVirtualDiskFile(tempDir.resolve("z80-disk0.img").toString());
		CpmFileSystem fs = new CpmFileSystem(drive0);
		fs.format();
		fs.createFile("SAMPLE.TXT", sampleTxt);
		machine.floppyController().attachDrive(0, drive0);

		// DUMP.COM itself is loaded directly to $0100 - real CP/M's own TPA
		// entry point - standing in for "a shell already loaded this real,
		// unmodified program and transferred control" (this feature
		// deliberately doesn't build a real CCP - see this feature's own
		// planning notes). CpmCompat.z80 (the dispatcher) still needs
		// loading explicitly here, same as bootFixture() does for the
		// other two tests - this one doesn't go through bootFixture()
		// itself since DUMP.COM isn't a fixture this project wrote. $0000
		// is set up as a plain self-loop (real CP/M's WBOOT vector) purely
		// defensively - unlike the hand-written Phase 1/2 fixtures,
		// DUMP.COM's own control flow isn't something this project wrote,
		// so there's no a-priori guarantee it never jumps there.
		machine.loadIntoMemory("CpmCompat.z80");
		machine.memoryBus().write(0x0000, (byte) 0xC3); // JP 0000H (self-loop)
		machine.memoryBus().write(0x0001, (byte) 0x00);
		machine.memoryBus().write(0x0002, (byte) 0x00);
		for (int i = 0; i < dumpCom.length; i++) {
			machine.memoryBus().write(0x0100 + i, dumpCom[i]);
		}

		// Real CCP's own job before transferring control to a program: the
		// raw command tail at $0080 (length byte + text, leading space
		// included) and FCB1 pre-parsed at $005C - DUMP.COM's own first
		// real BDOS call (Open File, DE=$5C) confirms it relies on the
		// pre-parsed FCB, not a re-parse of $0080 itself. Found by tracing
		// DUMP.COM with a throwaway diagnostic harness before writing this
		// test - it crashed without a real argument, the same way PAGE.COM
		// did, until this was added.
		String tail = " SAMPLE.TXT";
		machine.memoryBus().write(0x0080, (byte) tail.length());
		for (int i = 0; i < tail.length(); i++) {
			machine.memoryBus().write(0x0081 + i, (byte) tail.charAt(i));
		}
		byte[] fcb1Name = "SAMPLE  TXT".getBytes();
		for (int i = 0; i < fcb1Name.length; i++) {
			machine.memoryBus().write(0x005D + i, fcb1Name[i]);
		}

		callBios(0xF006); // BIOS_VIDEO_INIT - a real CCP would have done this once at boot
		cpu.pc = 0x0100;
		runSteps(60_000);

		// SAMPLE.TXT's own real first 32 bytes (confirmed via the
		// already-proven CpmImageReader.readFile(), not assumed) are 16
		// spaces followed by "       Introduction to VEDIT" - DUMP.COM's
		// real, correctly-formatted hex dump of them, produced entirely
		// through real CALL 5 Console Output calls into the existing
		// BIOS_PUTCH.
		assertEquals("0000 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20",
				machine.video().getRow(1).trim());
		assertEquals("0010 20 20 20 20 20 20 20 49 6E 74 72 6F 64 75 63 74",
				machine.video().getRow(2).trim());
	}
}
