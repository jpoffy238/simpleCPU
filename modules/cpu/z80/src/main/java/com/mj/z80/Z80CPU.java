package com.mj.z80;

import com.mj.Devices.PBus;
import com.mj.exceptions.DeviceUnavailable;
import com.mj.exceptions.ROException;
import com.mj.exceptions.illegalAddressException;
import com.mj.exceptions.illegalOpCodeException;

/** The Z80's register file plus the two independent buses a real Z80 system
 * has (see Z80Machine) - memoryBus for every ordinary instruction, portBus
 * for IN/OUT alone. Deliberately not extending Thread or offering a run()
 * loop yet (unlike the 6502 side's CPU) - that's tied to a real bootable
 * Z80 machine with real interrupt-raising devices, which doesn't exist
 * yet. step() executes exactly one instruction; a caller (today, a test;
 * eventually something like CPUMain's own run loop) calls it in a loop,
 * calling serviceInterrupts() first each time so a pending NMI/INT is
 * handled before the next instruction decodes. */
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

	/** Interrupt vector / memory refresh - plain 8-bit registers, never
	 * paired into a RegisterPair (there's no real `IR` combined addressing
	 * mode). Declared now since LD I,A/LD R,A need somewhere to write, even
	 * though nothing reads I for its real hardware purpose yet (interrupt
	 * mode 2's vector table lookup) or R for its own (DRAM refresh timing -
	 * meaningless to an emulator anyway). LD A,I/LD A,R are deliberately
	 * not implemented yet - unlike every other LD, they set real flags
	 * (P/V from IFF2, the interrupt-enable flip-flop), which doesn't exist
	 * until EI/DI/interrupt handling does. */
	public int i;
	public int r;

	public int pc;
	public int sp;

	/** The two interrupt enable flip-flops. IFF1 is the one that actually
	 * gates whether a maskable interrupt is accepted; IFF2 is just a backup
	 * copy, restored into IFF1 by RETN after an NMI (which only ever clears
	 * IFF1, never IFF2) and separately readable as LD A,I/LD A,R's P/V bit.
	 * Both start false - real hardware powers up with interrupts disabled. */
	public boolean iff1;
	public boolean iff2;

	/** Set by IM 0/1/2 (SetInterruptMode), read by serviceInterrupts() when
	 * a maskable interrupt actually needs handling. Starts at 0, the real
	 * power-on default - see serviceInterrupts()'s own comment for why
	 * leaving it there and then raising a maskable interrupt throws rather
	 * than silently doing something wrong. */
	public int interruptMode;

	/** Set by EI; consumed by the very next call to serviceInterrupts(),
	 * which simply skips that one check - guaranteeing the instruction
	 * immediately after EI always runs before any interrupt can land, even
	 * though IFF1/IFF2 are already true by then. This is what makes the
	 * common EI;RET ISR-exit idiom safe on real hardware, and the only
	 * reason EI doesn't just set IFF1/IFF2 and leave it at that. */
	public boolean suppressInterruptCheckOnce;

	/** The 8-bit vector a real IM2 device places on the data bus during the
	 * interrupt-acknowledge cycle, used as the low byte of the vector-table
	 * pointer (I is the high byte). No real device supplies this yet - no
	 * Z80 machine with real daisy-chained peripherals exists - so a caller
	 * (today, a test; eventually a real device) sets this directly before
	 * raising portBus's interrupt line. */
	public int pendingInterruptVector;

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

	/** Services a pending NMI or (if IFF1 allows it) maskable interrupt,
	 * pushing the current PC and jumping to the handler. Meant to be called
	 * before each step(), so EI's one-instruction grace period
	 * (suppressInterruptCheckOnce) can suppress exactly one check first.
	 * Returns true if an interrupt was serviced, which a driving loop
	 * should treat as having used this turn rather than also calling
	 * step() right after.
	 *
	 * Only portBus's interrupt lines are checked - interrupt-capable
	 * peripherals live in I/O space in this design, the same convention as
	 * the 6502 side routing every device interrupt through its VIA.
	 *
	 * NMI always wins over a maskable interrupt, is serviced regardless of
	 * IFF1, and clears only IFF1 (not IFF2) - see ReturnFromNmi.
	 *
	 * IM 0 (the interrupting device injecting an arbitrary instruction
	 * byte, not a vector) is not implemented - every real IM0 use is just
	 * RST n anyway, and IM 1 already reproduces the single most common
	 * case (RST 38H). Leaving interruptMode at its power-on default of 0
	 * and then raising a maskable interrupt anyway throws, rather than
	 * silently doing something wrong - call IM 1 or IM 2 first. */
	public boolean serviceInterrupts() throws illegalAddressException, DeviceUnavailable, ROException {
		if (suppressInterruptCheckOnce) {
			suppressInterruptCheckOnce = false;
			return false;
		}
		if (portBus.IsNMInterruptRaised()) {
			pushPC();
			iff1 = false;
			pc = 0x0066;
			portBus.clearNMInterupt();
			return true;
		}
		if (iff1 && portBus.IsInterruptRaised()) {
			if (interruptMode == 0) {
				throw new UnsupportedOperationException(
						"IM 0 interrupt servicing is not implemented - call IM 1 or IM 2 before raising a maskable interrupt");
			}
			iff1 = false;
			iff2 = false;
			pushPC();
			if (interruptMode == 1) {
				pc = 0x0038;
			} else {
				int vectorAddress = ((i & 0xff) << 8) | (pendingInterruptVector & 0xff);
				int low = memoryBus.read(vectorAddress) & 0xff;
				int high = memoryBus.read((vectorAddress + 1) & 0xffff) & 0xff;
				pc = (high << 8) | low;
			}
			portBus.clearInterupt();
			return true;
		}
		return false;
	}

	private void pushPC() throws illegalAddressException, DeviceUnavailable, ROException {
		sp = (sp - 1) & 0xffff;
		memoryBus.write(sp, (byte) ((pc >> 8) & 0xff));
		sp = (sp - 1) & 0xffff;
		memoryBus.write(sp, (byte) (pc & 0xff));
	}
}
