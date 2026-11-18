package com.mj.z80;

import com.mj.exceptions.DeviceUnavailable;
import com.mj.exceptions.illegalAddressException;
import com.mj.exceptions.illegalOpCodeException;

/** Walks the Z80's multi-byte prefix cascade, pulling bytes from the CPU one
 * at a time (each already advancing PC - see Z80CPU.fetchByte()) until a
 * real opcode table slot is identified:
 *
 *   no prefix   -&gt; base[opcode]
 *   $CB         -&gt; cb[opcode]
 *   $ED         -&gt; ed[opcode]
 *   $DD         -&gt; dd[opcode], OR if that byte is $CB: one more byte (the
 *                  displacement - read BEFORE the final opcode, unlike
 *                  every other prefixed form) then ddCb[finalOpcode]
 *   $FD         -&gt; fd[opcode] / fdCb[finalOpcode], mirroring $DD
 *
 * Each table is a flat 256-slot array, populated via the setters below
 * (nothing populates them yet - that's the next phase, one instruction
 * family at a time). A null slot is "undefined" and decode() throws
 * illegalOpCodeException for it, same contract as the 6502 side's
 * cpu001decoder - not an attempt to replicate real hardware's quirky
 * undefined-opcode behavior (deliberately out of scope for now).
 *
 * dd/fd are meant to be built from the same parameterized logic (an
 * instruction shared between both tables learns which index register, IX
 * or IY, it's working with via a closure captured when the table was
 * built - not by asking the CPU at execute time), so this class doesn't
 * need to know or care that the two tables are related; it only cares that
 * dd and ddCb are already its own distinct 256-slot arrays. The one piece
 * of real per-call state that can't be baked into a closure this way -
 * DD-CB/FD-CB's displacement byte, since its value is genuinely different
 * every time - is threaded through Z80CPU.pendingDisplacement instead. */
public class Z80InstructionDecoder implements Z80Decoder {
	private static final int TABLE_SIZE = 256;
	private static final int PREFIX_CB = 0xCB;
	private static final int PREFIX_ED = 0xED;
	private static final int PREFIX_DD = 0xDD;
	private static final int PREFIX_FD = 0xFD;

	private final Z80Instruction[] base = new Z80Instruction[TABLE_SIZE];
	private final Z80Instruction[] cb = new Z80Instruction[TABLE_SIZE];
	private final Z80Instruction[] ed = new Z80Instruction[TABLE_SIZE];
	private final Z80Instruction[] dd = new Z80Instruction[TABLE_SIZE];
	private final Z80Instruction[] fd = new Z80Instruction[TABLE_SIZE];
	private final Z80Instruction[] ddCb = new Z80Instruction[TABLE_SIZE];
	private final Z80Instruction[] fdCb = new Z80Instruction[TABLE_SIZE];

	public void setBase(int opcode, Z80Instruction instruction) {
		base[opcode & 0xff] = instruction;
	}

	public void setCB(int opcode, Z80Instruction instruction) {
		cb[opcode & 0xff] = instruction;
	}

	public void setED(int opcode, Z80Instruction instruction) {
		ed[opcode & 0xff] = instruction;
	}

	public void setDD(int opcode, Z80Instruction instruction) {
		dd[opcode & 0xff] = instruction;
	}

	public void setFD(int opcode, Z80Instruction instruction) {
		fd[opcode & 0xff] = instruction;
	}

	public void setDDCB(int opcode, Z80Instruction instruction) {
		ddCb[opcode & 0xff] = instruction;
	}

	public void setFDCB(int opcode, Z80Instruction instruction) {
		fdCb[opcode & 0xff] = instruction;
	}

	@Override
	public Z80Instruction decode(Z80CPU cpu) throws illegalOpCodeException, illegalAddressException, DeviceUnavailable {
		int first = cpu.fetchByte() & 0xff;
		switch (first) {
		case PREFIX_CB:
			return lookup(cb, cpu.fetchByte() & 0xff);
		case PREFIX_ED:
			return lookup(ed, cpu.fetchByte() & 0xff);
		case PREFIX_DD:
			return decodeIndexed(cpu, dd, ddCb);
		case PREFIX_FD:
			return decodeIndexed(cpu, fd, fdCb);
		default:
			return lookup(base, first);
		}
	}

	private Z80Instruction decodeIndexed(Z80CPU cpu, Z80Instruction[] table, Z80Instruction[] cbTable)
			throws illegalOpCodeException, illegalAddressException, DeviceUnavailable {
		int next = cpu.fetchByte() & 0xff;
		if (next != PREFIX_CB) {
			return lookup(table, next);
		}
		// Real Z80 byte order: displacement comes before the final opcode
		// here, unlike every other prefixed form (where any displacement an
		// instruction needs is read by that instruction itself, after it's
		// already been identified).
		cpu.pendingDisplacement = cpu.fetchByte();
		return lookup(cbTable, cpu.fetchByte() & 0xff);
	}

	private Z80Instruction lookup(Z80Instruction[] table, int opcode) throws illegalOpCodeException {
		Z80Instruction instruction = table[opcode & 0xff];
		if (instruction == null) {
			throw new illegalOpCodeException();
		}
		return instruction;
	}
}
