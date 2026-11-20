package com.mj.Devices;

/** A device that can supply a real Z80 IM2 interrupt vector when asked -
 * registered with a PBus in priority order via
 * PBus.registerInterruptVectorSource() (first registered = highest priority,
 * mirroring a real daisy chain's "closest to the CPU wins" convention).
 * Z80CPU.serviceInterrupts() calls PBus.acknowledgePendingVector() instead of
 * trusting a manually-set field once a real source is registered - see that
 * field's own comment. 6502-side code never touches this; IM2 is Z80-only.
 *
 * Implemented by VectoredInterruptController, not by an ordinary interrupt-
 * raising device (FloppyController, say) directly - see that class's own
 * comment for why a real floppy controller never held a vector itself. */
public interface InterruptVectorSource {

	/** True if this source currently has an unacknowledged interrupt request
	 * pending. */
	boolean hasPendingInterrupt();

	/** This source's current vector byte - only meaningful while
	 * hasPendingInterrupt() is true. Reading it does not itself acknowledge
	 * anything (see acknowledge()) - kept as a separate step so a caller can
	 * inspect without a side effect if it ever needs to. */
	int vector();

	/** Clears this source's own pending-request state. Called by
	 * PBus.acknowledgePendingVector() once it has chosen this source as the
	 * winner - presenting the vector is itself what acknowledges a real
	 * peripheral's interrupt during the hardware's own interrupt-acknowledge
	 * cycle, clearing its internal pending flip-flop as a side effect. */
	void acknowledge();
}
