package com.mj.z80.instructions;

import com.mj.z80.Z80InstructionDecoder;

/** Populates EI/DI/HALT (base table, $FB/$F3/$76) and IM 0/1/2, RETN/RETI,
 * LD A,I/LD A,R (all ED table, $46/$56/$5E, $45/$4D, $57/$5F) - the
 * interrupt-control instructions, HALT included since it only makes sense
 * in terms of them (it's the instruction LoadRegisterTable's own comment
 * carves $76 out for). See Z80CPU's iff1/iff2/interruptMode/halted fields
 * and serviceInterrupts() for the actual servicing mechanism these
 * configure. */
public final class InterruptTable {
	private InterruptTable() {
	}

	public static void populate(Z80InstructionDecoder decoder) {
		decoder.setBase(0xF3, DisableInterrupts.INSTANCE);
		decoder.setBase(0xFB, EnableInterrupts.INSTANCE);
		decoder.setBase(0x76, Halt.INSTANCE);

		decoder.setED(0x46, SetInterruptMode.MODE_0);
		decoder.setED(0x56, SetInterruptMode.MODE_1);
		decoder.setED(0x5E, SetInterruptMode.MODE_2);

		decoder.setED(0x45, ReturnFromNmi.INSTANCE);
		decoder.setED(0x4D, ReturnFromInterrupt.INSTANCE);

		decoder.setED(0x57, LoadAFromI.INSTANCE);
		decoder.setED(0x5F, LoadAFromR.INSTANCE);
	}
}
