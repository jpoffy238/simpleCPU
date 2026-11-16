package com.mj.Firmware.Table;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.mj.cpu001.CPU;
import com.mj.exceptions.illegalOpCodeException;
import com.mj.util.CPU_CreateUtil;

/** Byte values with no entry in DecoderMap (real 6502 undocumented/illegal opcodes this
 * emulator doesn't implement - $02, in particular, is never registered in OpCodes.java).
 * step() propagates the decode failure directly; run() (CPU's own Thread.run(), called
 * here synchronously rather than via start()) catches it internally and stops cleanly
 * instead of letting it escape the run loop. */
public class Test_IllegalOpcodes {
	private CPU c;

	@BeforeEach
	public void setup() throws Exception {
		c = CPU_CreateUtil.getCPU();
		c.step();            // consume power-on reset
		c.pc = 0x1000;
	}

	@Test
	public void stepThrowsOnAGenuinelyUnmappedOpcode() throws Exception {
		c.bus.write(0x1000, (byte) 0x02);
		assertThrows(illegalOpCodeException.class, () -> c.step());
		// fetchInstruction() advances the PC before decode runs, so it's past the
		// opcode byte even though decode itself failed
		assertEquals(0x1001, c.pc);
	}

	@Test
	public void runStopsCleanlyInsteadOfPropagatingOnAnUnmappedOpcode() throws Exception {
		c.bus.write(0x1000, (byte) 0x02);
		c.run();              // one failed fetch/decode sets RUN=false and the loop exits
		assertEquals(0x1001, c.pc, "run() still advances past the fetch before giving up");
	}
}
