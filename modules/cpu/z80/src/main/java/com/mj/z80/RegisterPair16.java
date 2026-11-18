package com.mj.z80;

/** BC, DE, HL, or SP - the 16-bit register a `dd`/`pp`-coded opcode (LD
 * dd,nn; the ED-prefixed LD dd,(nn)/LD (nn),dd family) refers to via its
 * 2-bit field. Exposes only 16-bit get/set, unlike Register8/RegisterPair's
 * 8-bit halves - nothing in this family needs those, and SP (a plain int
 * on Z80CPU, not a RegisterPair - no 8-bit half access is ever legal on
 * real hardware) couldn't support them even if something did. */
public enum RegisterPair16 {
	BC(0, cpu -> cpu.bc.get16(), (cpu, v) -> cpu.bc.set16(v)),
	DE(1, cpu -> cpu.de.get16(), (cpu, v) -> cpu.de.set16(v)),
	HL(2, cpu -> cpu.hl.get16(), (cpu, v) -> cpu.hl.set16(v)),
	SP(3, cpu -> cpu.sp, (cpu, v) -> cpu.sp = v & 0xffff);

	@FunctionalInterface
	private interface Getter {
		int get(Z80CPU cpu);
	}

	@FunctionalInterface
	private interface Setter {
		void set(Z80CPU cpu, int value);
	}

	private final int code;
	private final Getter getter;
	private final Setter setter;

	RegisterPair16(int code, Getter getter, Setter setter) {
		this.code = code;
		this.getter = getter;
		this.setter = setter;
	}

	public int code() {
		return code;
	}

	public int get16(Z80CPU cpu) {
		return getter.get(cpu);
	}

	public void set16(Z80CPU cpu, int value) {
		setter.set(cpu, value);
	}
}
