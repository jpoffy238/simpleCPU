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

	// ---- Stores ----------------------------------------------------------------
	public static final Operation STA = Operation.store("STA", "none",
			"M = A - Stores the contents of the accumulator into memory.", c -> c.a.get());

	public static final Operation STX = Operation.store("STX", "none",
			"M = X - Stores the contents of the X register into memory.", c -> c.x.get());

	public static final Operation STY = Operation.store("STY", "none",
			"M = Y - Stores the contents of the Y register into memory.", c -> c.y.get());

	// ---- Logical ---------------------------------------------------------------
	public static final Operation AND = Operation.read("AND", "Z,N",
			"A,Z,N = A&M - A logical AND is performed, bit by bit, on the accumulator contents using the contents of a byte of memory.",
			(c, v) -> logical(c, c.a.get() & v));

	public static final Operation ORA = Operation.read("ORA", "Z,N",
			"A,Z,N = A|M - An inclusive OR is performed, bit by bit, on the accumulator contents using the contents of a byte of memory.",
			(c, v) -> logical(c, c.a.get() | v));

	public static final Operation EOR = Operation.read("EOR", "Z,N",
			"A,Z,N = A^M - An exclusive OR is performed, bit by bit, on the accumulator contents using the contents of a byte of memory.",
			(c, v) -> logical(c, c.a.get() ^ v));

	private static void logical(com.mj.cpu001.CPU c, int result) {
		Registers.put(c.a, result);
		Registers.setNZ(c, result);
	}
}
