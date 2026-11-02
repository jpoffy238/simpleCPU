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

	// ---- Increments and decrements ---------------------------------------------
	public static final Operation INC = Operation.modify("INC", "Z,N",
			"M,Z,N = M+1 - Adds one to the value held at a specified memory location setting the zero and negative flags as appropriate.",
			(c, v) -> {
				int r = (v + 1) & 0xff;
				Registers.setNZ(c, r);
				return r;
			});

	public static final Operation DEC = Operation.modify("DEC", "Z,N",
			"M,Z,N = M-1 - Subtracts one from the value held at a specified memory location setting the zero and negative flags as appropriate.",
			(c, v) -> {
				int r = (v - 1) & 0xff;
				Registers.setNZ(c, r);
				return r;
			});

	public static final Operation INX = Operation.implied("INX", "Z,N",
			"X,Z,N = X+1 - Adds one to the X register setting the zero and negative flags as appropriate.",
			c -> {
				int r = (c.x.get() + 1) & 0xff;
				Registers.put(c.x, r);
				Registers.setNZ(c, r);
			});

	public static final Operation INY = Operation.implied("INY", "Z,N",
			"Y,Z,N = Y+1 - Adds one to the Y register setting the zero and negative flags as appropriate.",
			c -> {
				int r = (c.y.get() + 1) & 0xff;
				Registers.put(c.y, r);
				Registers.setNZ(c, r);
			});

	public static final Operation DEX = Operation.implied("DEX", "Z,N",
			"X,Z,N = X-1 - Subtracts one from the X register setting the zero and negative flags as appropriate.",
			c -> {
				int r = (c.x.get() - 1) & 0xff;
				Registers.put(c.x, r);
				Registers.setNZ(c, r);
			});

	public static final Operation DEY = Operation.implied("DEY", "Z,N",
			"Y,Z,N = Y-1 - Subtracts one from the Y register setting the zero and negative flags as appropriate.",
			c -> {
				int r = (c.y.get() - 1) & 0xff;
				Registers.put(c.y, r);
				Registers.setNZ(c, r);
			});
}
