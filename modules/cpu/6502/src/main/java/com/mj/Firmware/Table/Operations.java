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

	public static final Operation LDX = Operation.read("LDX", "Z,N",
			"X,Z,N = M - Loads a byte of memory into the X register setting the zero and negative flags as appropriate.",
			(c, v) -> {
				Registers.put(c.x, v);
				Registers.setNZ(c, v);
			});

	public static final Operation LDY = Operation.read("LDY", "Z,N",
			"Y,Z,N = M - Loads a byte of memory into the Y register setting the zero and negative flags as appropriate.",
			(c, v) -> {
				Registers.put(c.y, v);
				Registers.setNZ(c, v);
			});
}
