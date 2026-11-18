package com.mj.Devices;

/** A single interrupt line a device can pull, abstracted away from
 * whatever's actually wired to the other end of it - a VIA6522's own
 * requestInterrupt(flag) for one fixed flag, or a bus's raiseInterupt()
 * directly. One method, so any existing interrupt-capable chip or bus can
 * satisfy it with a lambda at the call site, rather than needing its own
 * adapter class. Lets a CPU-agnostic device like FloppyController depend
 * on "something it can interrupt," not on which specific interrupt
 * controller a given machine happens to use - a 6502 machine's VIA6522 is
 * real period hardware for that CPU, but a Z80 machine has no business
 * needing one just to share this class. */
@FunctionalInterface
public interface InterruptLine {
	void raiseInterrupt();
}
