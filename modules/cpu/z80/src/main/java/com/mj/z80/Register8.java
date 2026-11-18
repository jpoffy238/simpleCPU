package com.mj.z80;

/** One of the Z80's seven real 8-bit registers - B, C, D, E, H, L, A. Never
 * (HL)/(IX+d)/(IY+d) or an immediate byte - those are different operand
 * shapes (a memory access, or a literal baked into the instruction stream)
 * that don't fit "read/write one of RegisterPair's own halves" the way a
 * plain register does, so they get their own accessor types once an
 * instruction family actually needs them, rather than being forced into
 * this one. get()/set() reach into whichever RegisterPair the register is
 * actually the high or low half of. */
public enum Register8 {
	B(cpu -> cpu.bc.hi(), (cpu, v) -> cpu.bc.setHi(v)),
	C(cpu -> cpu.bc.lo(), (cpu, v) -> cpu.bc.setLo(v)),
	D(cpu -> cpu.de.hi(), (cpu, v) -> cpu.de.setHi(v)),
	E(cpu -> cpu.de.lo(), (cpu, v) -> cpu.de.setLo(v)),
	H(cpu -> cpu.hl.hi(), (cpu, v) -> cpu.hl.setHi(v)),
	L(cpu -> cpu.hl.lo(), (cpu, v) -> cpu.hl.setLo(v)),
	A(cpu -> cpu.af.hi(), (cpu, v) -> cpu.af.setHi(v));

	@FunctionalInterface
	private interface Getter {
		int get(Z80CPU cpu);
	}

	@FunctionalInterface
	private interface Setter {
		void set(Z80CPU cpu, int value);
	}

	private final Getter getter;
	private final Setter setter;

	Register8(Getter getter, Setter setter) {
		this.getter = getter;
		this.setter = setter;
	}

	public int get(Z80CPU cpu) {
		return getter.get(cpu);
	}

	public void set(Z80CPU cpu, int value) {
		setter.set(cpu, value);
	}
}
