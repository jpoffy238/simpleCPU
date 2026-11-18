package com.mj.z80;

import com.mj.Devices.PBus;
import com.mj.exceptions.DeviceUnavailable;
import com.mj.exceptions.ROException;
import com.mj.exceptions.illegalAddressException;
import com.mj.exceptions.illegalOpCodeException;

/** The Z80's register file plus the two independent buses a real Z80 system
 * has (see Z80Machine) - memoryBus for every ordinary instruction, portBus
 * for IN/OUT alone. Deliberately not extending Thread or offering a run()
 * loop yet (unlike the 6502 side's CPU) - that's tied to real interrupt
 * handling (NMI, IM 0/1/2), which is out of scope until the instructions
 * that configure it (EI/DI/IM) exist. step() executes exactly one
 * instruction; a caller (today, a test; eventually something like
 * CPUMain's own run loop) calls it in a loop. */
public class Z80CPU {
	public final PBus memoryBus;
	public final PBus portBus;
	private final Z80Decoder decoder;

	/** Main register set. */
	public final RegisterPair af = new RegisterPair();
	public final RegisterPair bc = new RegisterPair();
	public final RegisterPair de = new RegisterPair();
	public final RegisterPair hl = new RegisterPair();

	/** Shadow register set, swapped in via EX AF,AF'/EXX - not yet touched by
	 * anything (those instructions don't exist yet), but declared now since
	 * it's part of the register file regardless of which instructions are
	 * implemented first. */
	public final RegisterPair af2 = new RegisterPair();
	public final RegisterPair bc2 = new RegisterPair();
	public final RegisterPair de2 = new RegisterPair();
	public final RegisterPair hl2 = new RegisterPair();

	public final RegisterPair ix = new RegisterPair();
	public final RegisterPair iy = new RegisterPair();

	public int pc;
	public int sp;

	/** DD-CB/FD-CB's displacement byte - the one piece of per-call state that
	 * can't be baked into a table slot the way "which index register, IX or
	 * IY" can (see Z80InstructionDecoder's class comment): the decoder has
	 * to read it before it can even identify which instruction to dispatch
	 * to, so by the time that instruction's own execute() runs, PC has
	 * already moved past it. Set by the decoder immediately before
	 * returning a DD_CB/FD_CB-table instruction; meaningless otherwise. */
	public byte pendingDisplacement;

	public Z80CPU(PBus memoryBus, PBus portBus, Z80Decoder decoder) {
		this.memoryBus = memoryBus;
		this.portBus = portBus;
		this.decoder = decoder;
	}

	/** Reads the byte at PC and advances PC by one - the single primitive
	 * every fetch (opcode, prefix, displacement, immediate/address operand)
	 * goes through, each byte advancing PC the moment it's consumed. This is
	 * a deliberate departure from the 6502 side's convention (read operand
	 * bytes relative to a not-yet-advanced PC, then bump PC once by the
	 * whole instruction's size at the end of execute()) - the Z80's prefix
	 * cascade consumes a variable 1-4 bytes before an instruction is even
	 * identified, which makes "add up how many bytes decode must have
	 * consumed" error-prone; advancing immediately as each byte is read
	 * avoids needing to separately track that. */
	public byte fetchByte() throws illegalAddressException, DeviceUnavailable {
		byte value = memoryBus.read(pc);
		pc = (pc + 1) & 0xffff;
		return value;
	}

	/** Decodes and executes exactly one instruction. */
	public void step() throws illegalOpCodeException, illegalAddressException, DeviceUnavailable, ROException {
		Z80Instruction instruction = decoder.decode(this);
		instruction.execute(this);
	}
}
