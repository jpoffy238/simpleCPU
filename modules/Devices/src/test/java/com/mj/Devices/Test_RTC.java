package com.mj.Devices;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.mj.exceptions.ROException;

/** Checks RTC's own two modes in isolation, against a bare DeviceBus - the
 * 6502 side's own Firmware/FileDevice/Test_RTC covers the original
 * VIA-interrupt-driven path end to end through a real CPU; this one covers
 * the device itself, including the newer self-refreshing poll path neither
 * real machine's own BIOS_GET_TIME primitive needs a ticking Thread for. */
public class Test_RTC {
	private static final int BASE = 0x9025;

	private PBus bus;
	private RTC rtc;

	@BeforeEach
	public void setup() {
		bus = new DeviceBus();
	}

	@AfterEach
	public void teardown() {
		// Only one test actually starts the thread, but interrupting an
		// already-dead one is harmless - cheap insurance against leaking a
		// live thread into later tests either way.
		if (rtc != null) {
			rtc.interrupt();
		}
	}

	private long wallClockEpochSeconds() {
		return System.currentTimeMillis() / 1000L;
	}

	@Test
	public void readsALiveValueThroughPlainPollingEvenWithoutStartingItsOwnThread() throws Exception {
		rtc = new RTC(() -> { }, new AddressRange(BASE, BASE + 3));
		bus.registerDevice(rtc);

		long before = wallClockEpochSeconds();
		long epoch = 0;
		for (int i = 0; i < 4; i++) {
			epoch = (epoch << 8) | (bus.read(BASE + i) & 0xffL);
		}
		long after = wallClockEpochSeconds();

		assertTrue(epoch >= before && epoch <= after,
				"RTC read " + epoch + " should be between " + before + " and " + after);
	}

	@Test
	public void writingIsNotAllowed() {
		rtc = new RTC(() -> { }, new AddressRange(BASE, BASE + 3));
		bus.registerDevice(rtc);

		assertThrows(ROException.class, () -> bus.write(BASE, (byte) 0x42));
	}

	@Test
	public void theBackgroundThreadTicksAndRaisesTheGivenInterruptLineWhenStarted() throws Exception {
		AtomicBoolean interrupted = new AtomicBoolean(false);
		rtc = new RTC(() -> interrupted.set(true), new AddressRange(BASE, BASE + 3));
		bus.registerDevice(rtc);

		rtc.start();
		Thread.sleep(1100); // run() ticks once a second - give it one real tick

		assertTrue(interrupted.get(), "the interrupt line should have been raised by the ticking thread");
	}

	@Test
	public void aStartedThreadsPeriodicRefreshIsWhatReadReliesOnInsteadOfSelfRefreshing() throws Exception {
		// Once started, read() deliberately stops self-refreshing (see its own
		// comment) - this just confirms the thread's own now() calls alone are
		// enough to keep the register live, i.e. the mode switch didn't leave
		// the value frozen at construction time.
		rtc = new RTC(() -> { }, new AddressRange(BASE, BASE + 3));
		bus.registerDevice(rtc);
		long constructionEpoch = readEpoch();

		rtc.start();
		Thread.sleep(1100);
		long afterOneTick = readEpoch();

		assertTrue(afterOneTick > constructionEpoch,
				"the ticking thread should have advanced the register at least one second");
	}

	private long readEpoch() throws Exception {
		long epoch = 0;
		for (int i = 0; i < 4; i++) {
			epoch = (epoch << 8) | (bus.read(BASE + i) & 0xffL);
		}
		return epoch;
	}
}
