package com.mj.Firmware.Addressing;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.mj.cpu001.CPU;
import com.mj.util.CPU_CreateUtil;

/**
 * Edge cases where the 6502 wraps addresses: index arithmetic, zero-page
 * pointers, the NMOS JMP (indirect) page bug, branches and the PC itself.
 */
public class Test_AddressingEdgeCases {
	private CPU c;

	@BeforeEach
	public void setup() throws Exception {
		c = CPU_CreateUtil.getCPU();
		c.step();          // consume the pending power-on reset (MIH)
		c.pc = 0x1000;
	}

	private void poke(int addr, int value) throws Exception {
		c.bus.write(addr, (byte) value);
	}

	@Test
	public void absoluteXWrapsAt16Bits() throws Exception {
		// LDA $FFF0,X with X=$20 -> $0010 (not $10010)
		poke(0x1000, 0xBD); poke(0x1001, 0xF0); poke(0x1002, 0xFF);
		poke(0x0010, 0x5A);
		c.x.set(0x20);
		c.step();
		assertEquals(0x5A, c.a.get());
		assertEquals(0x1003, c.pc);
	}

	@Test
	public void absoluteYWrapsAt16Bits() throws Exception {
		// LDA $FFF0,Y with Y=$20 -> $0010
		poke(0x1000, 0xB9); poke(0x1001, 0xF0); poke(0x1002, 0xFF);
		poke(0x0010, 0x3C);
		c.y.set(0x20);
		c.step();
		assertEquals(0x3C, c.a.get());
	}

	@Test
	public void indirectYPointerWrapsInZeroPage() throws Exception {
		// LDA ($FF),Y : low byte from $FF, high byte from $00 (not $0100)
		poke(0x1000, 0xB1); poke(0x1001, 0xFF);
		poke(0x00FF, 0x00);
		poke(0x0000, 0x20);        // pointer = $2000
		poke(0x0100, 0x30);        // would give $3000 if the wrap were missed
		poke(0x2004, 0x77);
		poke(0x3004, 0x11);
		c.y.set(4);
		c.step();
		assertEquals(0x77, c.a.get());
	}

	@Test
	public void jmpIndirectDoesNotCarryAcrossPage() throws Exception {
		// JMP ($10FF): low from $10FF, high from $1000 (NMOS bug), not $1100
		poke(0x1000, 0x6C); poke(0x1001, 0xFF); poke(0x1002, 0x10);
		poke(0x10FF, 0x80);
		poke(0x1100, 0x50);
		// the high byte comes from $1000, which holds the JMP opcode $6C
		c.step();
		assertEquals(0x6C80, c.pc);
	}

	@Test
	public void jmpIndirectNormalCase() throws Exception {
		poke(0x1000, 0x6C); poke(0x1001, 0x00); poke(0x1002, 0x20);
		poke(0x2000, 0x34); poke(0x2001, 0x12);
		c.step();
		assertEquals(0x1234, c.pc);
	}

	@Test
	public void backwardBranchWrapsPastZero() throws Exception {
		// BNE -4 located at $0000: next=$0002, target=$FFFE
		c.pc = 0x0000;
		poke(0x0000, 0xD0); poke(0x0001, 0xFC);
		c.step();
		assertEquals(0xFFFE, c.pc);
	}

	@Test
	public void branchTakenAndNotTaken() throws Exception {
		poke(0x1000, 0xD0); poke(0x1001, 0x05);   // BNE +5
		c.ZFLAG.clear();
		c.step();
		assertEquals(0x1007, c.pc);

		c.pc = 0x1000;
		c.ZFLAG.set();
		c.step();
		assertEquals(0x1002, c.pc);
	}

	@Test
	public void branchesOnEachFlag() throws Exception {
		int[][] cases = { { 0x90 }, { 0xB0 }, { 0xF0 }, { 0x30 }, { 0x10 }, { 0x50 }, { 0x70 } };
		for (int[] cs : cases) {
			c.CFLAG.clear(); c.ZFLAG.clear(); c.NFLAG.clear(); c.OFLAG.clear();
			poke(0x1000, cs[0]); poke(0x1001, 0x02);
			c.pc = 0x1000;
			c.step();
			// with all flags clear: BCC, BPL, BVC take the branch; the others don't
			boolean expectTaken = cs[0] == 0x90 || cs[0] == 0x10 || cs[0] == 0x50;
			assertEquals(expectTaken ? 0x1004 : 0x1002, c.pc, String.format("opcode %02x", cs[0]));
		}
	}

	@Test
	public void overflowBranchesUseCorrectOpcodes() throws Exception {
		// BVC = $50 (branch when V clear), BVS = $70 (branch when V set)
		assertEquals(0x50, com.mj.Firmware.Framework.OpCodes.BVC.code() & 0xff);
		assertEquals(0x70, com.mj.Firmware.Framework.OpCodes.BVS.code() & 0xff);
		for (int op : new int[] { 0x50, 0x70 }) {
			for (boolean v : new boolean[] { false, true }) {
				if (v) c.OFLAG.set(); else c.OFLAG.clear();
				poke(0x1000, op); poke(0x1001, 0x02);
				c.pc = 0x1000;
				c.step();
				boolean taken = (op == 0x70) == v;
				assertEquals(taken ? 0x1004 : 0x1002, c.pc, String.format("op %02x V=%b", op, v));
			}
		}
	}
}
