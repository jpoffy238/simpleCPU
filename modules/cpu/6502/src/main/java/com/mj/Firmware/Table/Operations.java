package com.mj.Firmware.Table;

/** The catalogue of operations. Each is defined once and shared by every addressing mode. */
public final class Operations {
	private Operations() {
	}

	// ---- Loads -----------------------------------------------------------------
	public static final Operation LDA = Operation.read("LDA", "Z,N",
			"A,Z,N = M - Loads a byte of memory into the accumulator setting the zero and negative flags as appropriate.",
			(c, v) -> {
				Registers.put(c.a, v);
				Registers.setNZ(c, v);
			});
}
