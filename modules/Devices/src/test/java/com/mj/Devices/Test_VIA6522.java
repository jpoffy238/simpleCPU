package com.mj.Devices;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Checks the VIA's IFR/IER semantics against the real 6522: a source only
 * reaches the CPU's shared IRQ line while it is both pending (IFR) and
 * enabled (IER), IFR bit 7 mirrors that aggregate, and IER always reads bit 7
 * as 1 regardless of what was written. */
public class Test_VIA6522 {
	private static final int BASE = 0x9000;
	private static final int IFR = BASE + VIA6522.REG_IFR;
	private static final int IER = BASE + VIA6522.REG_IER;

	private PBus bus;
	private VIA6522 via;

	@BeforeEach
	public void setup() {
		bus = new DeviceBus();
		via = new VIA6522(bus, new AddressRange(BASE, BASE + 0xF));
		bus.registerDevice(via);
	}

	@Test
	public void requestWithoutEnableSetsFlagButDoesNotInterruptTheCpu() throws Exception {
		via.requestInterrupt(VIA6522.FLAG_CA1);

		assertFalse(bus.IsInterruptRaised(), "CA1 isn't enabled, so the CPU shouldn't see anything");
		assertEquals(VIA6522.FLAG_CA1, bus.read(IFR) & 0xff, "the flag itself is still recorded");
	}

	@Test
	public void enabledSourceRaisesTheSharedIrqLine() throws Exception {
		bus.write(IER, (byte) (0x80 | VIA6522.FLAG_T1)); // set-mode: enable T1

		via.requestInterrupt(VIA6522.FLAG_T1);

		assertTrue(bus.IsInterruptRaised());
		int ifr = bus.read(IFR) & 0xff;
		assertEquals(VIA6522.FLAG_IRQ | VIA6522.FLAG_T1, ifr, "bit 7 mirrors the enabled+pending aggregate");
	}

	@Test
	public void writingOnesToIfrClearsThoseFlagsAndDropsTheIrqLine() throws Exception {
		bus.write(IER, (byte) (0x80 | VIA6522.FLAG_CB1));
		via.requestInterrupt(VIA6522.FLAG_CB1);
		assertTrue(bus.IsInterruptRaised());

		bus.write(IFR, (byte) VIA6522.FLAG_CB1); // acknowledge

		assertFalse(bus.IsInterruptRaised());
		assertEquals(0, bus.read(IFR) & 0xff);
	}

	@Test
	public void disablingASourceDropsTheIrqLineEvenIfStillPending() throws Exception {
		bus.write(IER, (byte) (0x80 | VIA6522.FLAG_T2));
		via.requestInterrupt(VIA6522.FLAG_T2);
		assertTrue(bus.IsInterruptRaised());

		bus.write(IER, (byte) VIA6522.FLAG_T2); // clear-mode (bit 7 = 0): disable T2

		assertFalse(bus.IsInterruptRaised(), "disabling the source should drop the line even though IFR is unacked");
		assertEquals(VIA6522.FLAG_T2, bus.read(IFR) & 0xff, "the flag itself is untouched by masking");
	}

	@Test
	public void ierAlwaysReadsBit7HighRegardlessOfWhatWasWritten() throws Exception {
		bus.write(IER, (byte) VIA6522.FLAG_CA2); // clear-mode write, bit7=0 in the written byte

		assertEquals(0x80, bus.read(IER) & 0xff, "bit 7 always reads back as 1 on real hardware");
	}

	@Test
	public void secondPendingEnabledSourceKeepsTheLineUpAfterTheFirstIsAcked() throws Exception {
		bus.write(IER, (byte) (0x80 | VIA6522.FLAG_CA1 | VIA6522.FLAG_CB1));
		via.requestInterrupt(VIA6522.FLAG_CA1);
		via.requestInterrupt(VIA6522.FLAG_CB1);

		bus.write(IFR, (byte) VIA6522.FLAG_CA1); // ack just CA1

		assertTrue(bus.IsInterruptRaised(), "CB1 is still pending and enabled");
		assertEquals(VIA6522.FLAG_IRQ | VIA6522.FLAG_CB1, bus.read(IFR) & 0xff);
	}
}
