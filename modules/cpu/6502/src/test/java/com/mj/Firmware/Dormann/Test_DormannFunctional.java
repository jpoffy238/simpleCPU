package com.mj.Firmware.Dormann;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import com.mj.Devices.AddressRange;
import com.mj.Devices.DeviceBus;
import com.mj.Devices.PBus;
import com.mj.Firmware.Framework.cpu001decoder;
import com.mj.cpu001.CPU;
import com.mj.memoryInterface.MemoryProbe;
import com.mj.memoryInterface.basicMemory;

/**
 * Klaus Dormann's 6502 functional test (https://github.com/Klaus2m5/6502_65C02_functional_tests, GPL-3.0).
 * <p>
 * The test is a 64 KB image that exercises every documented opcode and addressing mode, the flags, decimal
 * mode, BRK/RTI and the stack. It signals its result by parking the PC in a {@code jmp *} / branch-to-self
 * loop: at {@value #SUCCESS_PC} (hex) means every check passed, anywhere else is the failing check.
 * <p>
 * The image is not redistributed here. Download {@code bin_files/6502_functional_test.bin} (and optionally the
 * {@code .lst}) into {@code modules/cpu/6502/testdata/dormann/}, or point at it with
 * {@code -Ddormann.bin=/path/to/6502_functional_test.bin}. Run with {@code mvn test -DslowTests=true}.
 */
@Tag("slow")
@EnabledIfSystemProperty(named = "slowTests", matches = "true",
		disabledReason = "Runs ~30 million instructions. Opt in with: mvn test -DslowTests=true")
public class Test_DormannFunctional {
	static final int SUCCESS_PC = 0x3469;
	static final int START_PC = 0x0400;
	/** Safety budget; override with -Ddormann.max=N (useful for measuring speed). */
	static final long MAX_INSTRUCTIONS = Long.getLong("dormann.max", 200_000_000L);

	private static Path image() {
		String p = System.getProperty("dormann.bin", "testdata/dormann/6502_functional_test.bin");
		return Paths.get(p);
	}

	@Test
	public void passesTheFunctionalTest() throws Exception {
		Path bin = image();
		org.junit.jupiter.api.Assumptions.assumeTrue(Files.exists(bin),
				"Dormann image not found at " + bin.toAbsolutePath() + " - see the class comment");
		byte[] program = Files.readAllBytes(bin);
		assertEquals(65536, program.length, "expected a 64 KB image");

		PBus bus = new DeviceBus();
		basicMemory ram = new basicMemory(bus, new AddressRange(0, 0xffff), null, 0);
		bus.registerDevice(ram);
		CPU c = new CPU(bus, new cpu001decoder());
		System.arraycopy(program, 0, MemoryProbe.raw(ram), 0, 65536);
		bus.clearpowerOnReset();
		c.reset();
		c.pc = START_PC;

		long started = System.nanoTime();
		long executed = 0;
		int trapPc = -1;
		String problem = null;
		try {
			while (executed < MAX_INSTRUCTIONS) {
				int before = c.pc;
				c.step();
				executed++;
				if (c.pc == before) {            // jmp * or branch-to-self: the test's trap
					trapPc = before;
					break;
				}
			}
		} catch (Exception e) {
			problem = e.getClass().getSimpleName() + " at PC=" + String.format("%04x", c.pc);
			trapPc = c.pc;
		}
		double seconds = (System.nanoTime() - started) / 1e9;
		System.out.printf("DORMANN: %,d instructions in %.1f s (%.1f M instr/s), stopped at %04x%n", executed, seconds,
				executed / seconds / 1e6, trapPc);

		if (problem == null && trapPc == SUCCESS_PC) {
			return;                               // all tests passed
		}
		StringBuilder sb = new StringBuilder();
		sb.append(problem != null ? "CPU raised " + problem : "Trapped at " + String.format("%04x", trapPc))
				.append(String.format(" after %,d instructions.%n", executed));
		sb.append(String.format("test_case ($0200)=%02x  A=%02x X=%02x Y=%02x SP=%02x  C=%b Z=%b I=%b D=%b V=%b N=%b%n",
				bus.read(0x0200) & 0xff, c.a.get(), c.x.get(), c.y.get(), c.sp, c.CFLAG.isSet(), c.ZFLAG.isSet(),
				c.IFLAG.isSet(), c.DFLAG.isSet(), c.OFLAG.isSet(), c.NFLAG.isSet()));
		sb.append(listingContext(bin, trapPc));
		throw new AssertionError(sb.toString());
	}

	/** Lines of Dormann's assembler listing leading up to the trap, if the .lst sits beside the image. */
	private static String listingContext(Path bin, int pc) {
		Path lst = Paths.get(bin.toString().replaceAll("\\.bin$", ".lst"));
		if (!Files.exists(lst)) {
			return "(put 6502_functional_test.lst next to the image to see the failing source lines)";
		}
		try {
			List<String> lines = Files.readAllLines(lst);
			String key = String.format("%04x :", pc);
			for (int i = 0; i < lines.size(); i++) {
				if (lines.get(i).startsWith(key)) {
					StringBuilder sb = new StringBuilder("Listing around the trap:\n");
					for (int j = Math.max(0, i - 12); j <= Math.min(lines.size() - 1, i + 1); j++) {
						sb.append("  ").append(lines.get(j), 0, Math.min(110, lines.get(j).length())).append('\n');
					}
					return sb.toString();
				}
			}
			return "(address " + key + " not found in the listing)";
		} catch (Exception e) {
			return "(could not read the listing: " + e + ")";
		}
	}
}
