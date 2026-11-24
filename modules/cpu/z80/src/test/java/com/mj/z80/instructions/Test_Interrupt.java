package com.mj.z80.instructions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.mj.Devices.AddressRange;
import com.mj.Devices.VectoredInterruptController;
import com.mj.z80.Flags;
import com.mj.z80.Z80CPU;
import com.mj.z80.Z80InstructionDecoder;
import com.mj.z80.Z80Machine;

/** EI/DI, IM 0/1/2, RETN/RETI, LD A,I/LD A,R (see InterruptTable), and
 * Z80CPU.serviceInterrupts() itself - the actual NMI/maskable-interrupt
 * servicing mechanism those instructions configure. */
public class Test_Interrupt {
	private Z80Machine machine;
	private Z80CPU cpu;

	@BeforeEach
	public void setup() {
		machine = new Z80Machine();
		Z80InstructionDecoder decoder = new Z80InstructionDecoder();
		InterruptTable.populate(decoder);
		cpu = new Z80CPU(machine.memoryBus(), machine.portBus(), decoder);
	}

	private void runBase(int opcode) throws Exception {
		machine.memoryBus().write(0x1000, (byte) opcode);
		cpu.pc = 0x1000;
		cpu.step();
	}

	private void runED(int edOpcode) throws Exception {
		machine.memoryBus().write(0x1000, (byte) 0xED);
		machine.memoryBus().write(0x1001, (byte) edOpcode);
		cpu.pc = 0x1000;
		cpu.step();
	}

	// ---- EI / DI ----

	@Test
	public void eiSetsBothFlipFlopsAndSuppressesTheNextInterruptCheck() throws Exception {
		runBase(0xFB); // EI

		assertTrue(cpu.iff1);
		assertTrue(cpu.iff2);
		assertTrue(cpu.suppressInterruptCheckOnce);
	}

	@Test
	public void diClearsBothFlipFlops() throws Exception {
		cpu.iff1 = true;
		cpu.iff2 = true;

		runBase(0xF3); // DI

		assertFalse(cpu.iff1);
		assertFalse(cpu.iff2);
	}

	// ---- HALT ----

	@Test
	public void haltSetsTheHaltedFlag() throws Exception {
		runBase(0x76); // HALT

		assertTrue(cpu.halted);
	}

	@Test
	public void stepDoesNothingAtAllWhileHalted() throws Exception {
		cpu.halted = true;
		cpu.pc = 0x1000;
		machine.memoryBus().write(0x1000, (byte) 0x3C); // INC A - would change A/flags if it ran
		int before = cpu.af.hi();

		cpu.step();

		assertEquals(before, cpu.af.hi(), "a real halted CPU doesn't execute whatever instruction happens to follow");
		assertEquals(0x1000, cpu.pc, "PC shouldn't move either - there's nothing to fetch");
		assertTrue(cpu.halted);
	}

	@Test
	public void nmiWakesAHaltedCpuAndClearsTheFlag() throws Exception {
		runBase(0x76); // HALT - PC is already correctly past this opcode, same as any instruction
		int pcAfterHalt = cpu.pc;
		cpu.sp = 0x2000;
		machine.portBus().raiseNMInterupt();

		assertTrue(cpu.serviceInterrupts());

		assertFalse(cpu.halted, "a real interrupt arriving is exactly what wakes a halted CPU");
		assertEquals(0x0066, cpu.pc);
		int pushedReturnAddress = (machine.memoryBus().read(0x1FFE) & 0xff) | ((machine.memoryBus().read(0x1FFF) & 0xff) << 8);
		assertEquals(pcAfterHalt, pushedReturnAddress,
				"the pushed return address should resume right after HALT, not re-execute it");
	}

	@Test
	public void aServicedMaskableInterruptWakesAHaltedCpuToo() throws Exception {
		runBase(0x76); // HALT
		cpu.sp = 0x2000;
		cpu.iff1 = true;
		cpu.interruptMode = 1;
		machine.portBus().raiseInterupt();

		assertTrue(cpu.serviceInterrupts());

		assertFalse(cpu.halted);
		assertEquals(0x0038, cpu.pc);
	}

	@Test
	public void anUnservicedMaskableInterruptLeavesAHaltedCpuHaltedWhenIff1IsClear() throws Exception {
		runBase(0x76); // HALT
		cpu.iff1 = false;
		cpu.interruptMode = 1;
		machine.portBus().raiseInterupt();

		assertFalse(cpu.serviceInterrupts(), "iff1 is clear - this interrupt can't be accepted yet");
		assertTrue(cpu.halted, "still genuinely stuck - this is the real DI;HALT dead-halt case");
	}

	// ---- IM 0/1/2 ----

	@Test
	public void imSetsTheInterruptMode() throws Exception {
		runED(0x56); // IM 1
		assertEquals(1, cpu.interruptMode);

		runED(0x5E); // IM 2
		assertEquals(2, cpu.interruptMode);

		runED(0x46); // IM 0
		assertEquals(0, cpu.interruptMode);
	}

	// ---- RETN / RETI ----

	@Test
	public void retnPopsPcAndRestoresIff1FromIff2() throws Exception {
		cpu.sp = 0x2000;
		machine.memoryBus().write(0x2000, (byte) 0x34);
		machine.memoryBus().write(0x2001, (byte) 0x12); // return address 0x1234
		cpu.iff1 = false;
		cpu.iff2 = true; // what it was before the NMI that got us here

		runED(0x45); // RETN

		assertEquals(0x1234, cpu.pc);
		assertEquals(0x2002, cpu.sp);
		assertTrue(cpu.iff1);
	}

	@Test
	public void retiPopsPcAndLeavesTheFlipFlopsAlone() throws Exception {
		cpu.sp = 0x2000;
		machine.memoryBus().write(0x2000, (byte) 0x34);
		machine.memoryBus().write(0x2001, (byte) 0x12);
		cpu.iff1 = false;
		cpu.iff2 = false;

		runED(0x4D); // RETI

		assertEquals(0x1234, cpu.pc);
		assertFalse(cpu.iff1, "RETI must not touch IFF1/IFF2 - unlike RETN");
	}

	// ---- LD A,I / LD A,R ----

	@Test
	public void ldAFromISetsFlagsFromTheValueAndIff2() throws Exception {
		cpu.i = 0x80;
		cpu.iff2 = true;
		cpu.af.setLo(Flags.C); // sentinel - C preserved

		runED(0x57); // LD A,I

		assertEquals(0x80, cpu.af.hi());
		assertEquals(Flags.C | Flags.S | Flags.PV, cpu.af.lo());
	}

	@Test
	public void ldAFromIClearsPvWhenIff2IsClear() throws Exception {
		cpu.i = 0x00;
		cpu.iff2 = false;

		runED(0x57); // LD A,I

		assertEquals(Flags.Z, cpu.af.lo());
	}

	@Test
	public void ldAFromRBehavesTheSameWayAsLdAFromI() throws Exception {
		cpu.r = 0x05;
		cpu.iff2 = true;

		runED(0x5F); // LD A,R

		assertEquals(0x05, cpu.af.hi());
		assertEquals(Flags.PV, cpu.af.lo());
	}

	// ---- serviceInterrupts() ----

	@Test
	public void serviceInterruptsDoesNothingWhenNothingIsRaised() throws Exception {
		cpu.iff1 = true;
		cpu.interruptMode = 1;

		assertFalse(cpu.serviceInterrupts());
	}

	@Test
	public void serviceInterruptsIgnoresAMaskableInterruptWhenIff1IsClear() throws Exception {
		cpu.iff1 = false;
		cpu.interruptMode = 1;
		machine.portBus().raiseInterupt();

		assertFalse(cpu.serviceInterrupts());
		assertTrue(machine.portBus().IsInterruptRaised(), "an un-serviced interrupt should still be pending");
	}

	@Test
	public void serviceInterruptsHonorsTheOneInstructionSuppressionAfterEi() throws Exception {
		cpu.iff1 = true; // as EI would have just set it
		cpu.suppressInterruptCheckOnce = true;
		cpu.interruptMode = 1;
		machine.portBus().raiseInterupt();

		assertFalse(cpu.serviceInterrupts(), "the check right after EI must be skipped");
		assertFalse(cpu.suppressInterruptCheckOnce, "the suppression is consumed, not sticky");
		assertTrue(machine.portBus().IsInterruptRaised(), "the interrupt itself is still pending, just not serviced yet");

		assertTrue(cpu.serviceInterrupts(), "the very next check should service it normally");
	}

	@Test
	public void serviceInterruptsHandlesImOneByPushingPcAndJumpingToFixedVector() throws Exception {
		cpu.pc = 0x1234;
		cpu.sp = 0x2000;
		cpu.iff1 = true;
		cpu.interruptMode = 1;
		machine.portBus().raiseInterupt();

		boolean serviced = cpu.serviceInterrupts();

		assertTrue(serviced);
		assertEquals(0x0038, cpu.pc);
		assertEquals(0x1FFE, cpu.sp);
		assertEquals(0x34, machine.memoryBus().read(0x1FFE) & 0xff);
		assertEquals(0x12, machine.memoryBus().read(0x1FFF) & 0xff);
		assertFalse(cpu.iff1);
		assertFalse(cpu.iff2);
		assertFalse(machine.portBus().IsInterruptRaised(), "the line should be cleared once serviced");
	}

	@Test
	public void serviceInterruptsHandlesImTwoByReadingAVectorTablePointer() throws Exception {
		cpu.pc = 0x1234;
		cpu.sp = 0x2000;
		cpu.iff1 = true;
		cpu.interruptMode = 2;
		cpu.i = 0x40;
		cpu.pendingInterruptVector = 0x10;
		// the vector table entry at $4010/$4011 (low byte first) holds the
		// real ISR address $8000 - not the jump target itself
		machine.memoryBus().write(0x4010, (byte) 0x00);
		machine.memoryBus().write(0x4011, (byte) 0x80);
		machine.portBus().raiseInterupt();

		assertTrue(cpu.serviceInterrupts());

		assertEquals(0x8000, cpu.pc);
		assertEquals(0x1FFE, cpu.sp);
		assertEquals(0x34, machine.memoryBus().read(0x1FFE) & 0xff);
		assertEquals(0x12, machine.memoryBus().read(0x1FFF) & 0xff);
	}

	@Test
	public void serviceInterruptsPrefersARegisteredVectorSourceOverTheManualOverrideField() throws Exception {
		cpu.pc = 0x1234;
		cpu.sp = 0x2000;
		cpu.iff1 = true;
		cpu.interruptMode = 2;
		cpu.i = 0x40;
		cpu.pendingInterruptVector = 0x99; // must be ignored once a real source is registered
		VectoredInterruptController controller = new VectoredInterruptController(machine.portBus(),
				new AddressRange(0x05, 0x05));
		machine.portBus().registerDevice(controller);
		machine.portBus().write(0x05, (byte) 0x10); // software loads the vector, the real IM2 setup step
		machine.memoryBus().write(0x4010, (byte) 0x00);
		machine.memoryBus().write(0x4011, (byte) 0x80);

		controller.requestInterrupt();

		assertTrue(cpu.serviceInterrupts());
		assertEquals(0x8000, cpu.pc, "should use the controller's own vector ($10), not pendingInterruptVector");
		assertFalse(controller.hasPendingInterrupt(), "presenting the vector is itself the acknowledgment");
		assertFalse(machine.portBus().IsInterruptRaised());
	}

	@Test
	public void serviceInterruptsThrowsForImZeroRatherThanDoingSomethingSilentlyWrong() throws Exception {
		cpu.iff1 = true;
		cpu.interruptMode = 0;
		machine.portBus().raiseInterupt();

		assertThrows(UnsupportedOperationException.class, cpu::serviceInterrupts);
	}

	@Test
	public void serviceInterruptsHandlesNmiByPushingPcClearingOnlyIff1AndJumpingToFixedVector() throws Exception {
		cpu.pc = 0x1234;
		cpu.sp = 0x2000;
		cpu.iff1 = true;
		cpu.iff2 = true;
		machine.portBus().raiseNMInterupt();

		assertTrue(cpu.serviceInterrupts());

		assertEquals(0x0066, cpu.pc);
		assertEquals(0x1FFE, cpu.sp);
		assertEquals(0x34, machine.memoryBus().read(0x1FFE) & 0xff);
		assertEquals(0x12, machine.memoryBus().read(0x1FFF) & 0xff);
		assertFalse(cpu.iff1);
		assertTrue(cpu.iff2, "NMI entry must leave IFF2 alone - that's what RETN restores IFF1 from");
		assertFalse(machine.portBus().IsNMInterruptRaised());
	}

	@Test
	public void nmiIsServicedEvenWhenIff1IsFalse() throws Exception {
		cpu.pc = 0x1234;
		cpu.sp = 0x2000;
		cpu.iff1 = false;
		machine.portBus().raiseNMInterupt();

		assertTrue(cpu.serviceInterrupts());
		assertEquals(0x0066, cpu.pc);
	}

	@Test
	public void nmiTakesPriorityOverAPendingMaskableInterrupt() throws Exception {
		cpu.pc = 0x1234;
		cpu.sp = 0x2000;
		cpu.iff1 = true;
		cpu.interruptMode = 1;
		machine.portBus().raiseInterupt();
		machine.portBus().raiseNMInterupt();

		cpu.serviceInterrupts();

		assertEquals(0x0066, cpu.pc, "NMI must win over the pending maskable interrupt");
		assertTrue(machine.portBus().IsInterruptRaised(), "the maskable interrupt is still pending, just not serviced this time");
	}

	@Test
	public void nmiThenRetnRoundTripsBackToTheInterruptedCode() throws Exception {
		cpu.pc = 0x1234;
		cpu.sp = 0x2000;
		cpu.iff1 = true;
		cpu.iff2 = true;
		machine.portBus().raiseNMInterupt();
		cpu.serviceInterrupts(); // jumps to $0066, pc now there, iff1 cleared

		machine.memoryBus().write(cpu.pc, (byte) 0xED);
		machine.memoryBus().write(cpu.pc + 1, (byte) 0x45); // RETN
		cpu.step();

		assertEquals(0x1234, cpu.pc);
		assertTrue(cpu.iff1);
	}
}
