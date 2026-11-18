package com.mj.z80;

/** BC, DE, HL, or AF - the 16-bit register pair PUSH/POP's `qq` field
 * selects. A different grouping than RegisterPair16's `dd` (BC/DE/HL/SP):
 * PUSH/POP can reach AF (the only way to save/restore flags at all) but
 * never SP itself - pushing/popping the stack pointer makes no sense when
 * it's the very thing being adjusted. Returns an actual RegisterPair
 * (unlike RegisterPair16, which has to work around SP being a plain int)
 * since all four of these genuinely are one. */
public enum StackRegisterPair {
	BC(0, cpu -> cpu.bc),
	DE(1, cpu -> cpu.de),
	HL(2, cpu -> cpu.hl),
	AF(3, cpu -> cpu.af);

	@FunctionalInterface
	private interface Selector {
		RegisterPair get(Z80CPU cpu);
	}

	private final int code;
	private final Selector selector;

	StackRegisterPair(int code, Selector selector) {
		this.code = code;
		this.selector = selector;
	}

	public int code() {
		return code;
	}

	public RegisterPair get(Z80CPU cpu) {
		return selector.get(cpu);
	}
}
