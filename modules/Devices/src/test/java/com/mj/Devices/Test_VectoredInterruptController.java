package com.mj.Devices;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Checks VectoredInterruptController's two halves in isolation: the vector
 * register (an ordinary port write/read) and the InterruptVectorSource
 * contract DeviceBus.acknowledgePendingVector() relies on - Test_Interrupt
 * (modules/cpu/z80) covers the real Z80CPU-facing end-to-end path. */
public class Test_VectoredInterruptController {
	private static final int BASE = 0x05;

	private PBus bus;
	private VectoredInterruptController controller;

	@BeforeEach
	public void setup() {
		bus = new DeviceBus();
		controller = new VectoredInterruptController(bus, new AddressRange(BASE, BASE));
		bus.registerDevice(controller);
	}

	@Test
	public void startsWithNothingPending() {
		assertFalse(controller.hasPendingInterrupt());
		assertFalse(bus.IsInterruptRaised());
	}

	@Test
	public void writingThePortLoadsTheVectorRegister() throws Exception {
		bus.write(BASE, (byte) 0x42);

		assertEquals(0x42, bus.read(BASE) & 0xff);
		assertEquals(0x42, controller.vector());
	}

	@Test
	public void requestInterruptRaisesTheSharedLineAndMarksThisSourcePending() throws Exception {
		bus.write(BASE, (byte) 0x10);

		controller.requestInterrupt();

		assertTrue(bus.IsInterruptRaised());
		assertTrue(controller.hasPendingInterrupt());
		assertEquals(0x10, controller.vector());
	}

	@Test
	public void acknowledgePendingVectorReturnsTheVectorAndClearsThePendingFlag() throws Exception {
		bus.write(BASE, (byte) 0x10);
		controller.requestInterrupt();

		int vector = bus.acknowledgePendingVector();

		assertEquals(0x10, vector);
		assertFalse(controller.hasPendingInterrupt(), "presenting the vector is itself the acknowledgment");
	}

	@Test
	public void acknowledgePendingVectorReturnsMinusOneWhenNothingIsPending() {
		assertEquals(-1, bus.acknowledgePendingVector());
	}

	@Test
	public void resetClearsBothTheVectorAndAnyPendingRequest() throws Exception {
		bus.write(BASE, (byte) 0x10);
		controller.requestInterrupt();

		controller.reset();

		assertEquals(0, controller.vector());
		assertFalse(controller.hasPendingInterrupt());
	}
}
