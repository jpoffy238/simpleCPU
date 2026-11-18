package com.mj.z80;

/** IX or IY - which 16-bit index register an (IX+d)/(IY+d)-shaped
 * instruction addresses through. Mirrors Register8's own shape: get(cpu)
 * resolves to whichever CPU instance's own ix or iy field, rather than
 * capturing one specific RegisterPair at construction time - the same
 * instruction object (closing over just an IndexRegister constant) has to
 * work correctly no matter which Z80CPU it's later handed, since table
 * population happens once, long before any particular CPU exists. */
public enum IndexRegister {
	IX(cpu -> cpu.ix),
	IY(cpu -> cpu.iy);

	@FunctionalInterface
	private interface Selector {
		RegisterPair get(Z80CPU cpu);
	}

	private final Selector selector;

	IndexRegister(Selector selector) {
		this.selector = selector;
	}

	public RegisterPair get(Z80CPU cpu) {
		return selector.get(cpu);
	}
}
