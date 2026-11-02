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

	// ---- Shifts and rotates ----------------------------------------------------
	public static final Operation ASL = Operation.modify("ASL", "Z,C,N",
			"A,C = A*2 or M,C = M*2 - Shifts all the bits one place to the left. Bit 7 is shifted into the carry flag and bit 0 is set to zero.",
			(c, v) -> {
				int r = (v << 1) & 0xff;
				Registers.setCarry(c, (v & 0x80) != 0);
				Registers.setNZ(c, r);
				return r;
			});

	public static final Operation LSR = Operation.modify("LSR", "Z,C,N",
			"A,C = A/2 or M,C = M/2 - Shifts all the bits one place to the right. Bit 0 is shifted into the carry flag and bit 7 is set to zero.",
			(c, v) -> {
				int r = (v & 0xff) >> 1;
				Registers.setCarry(c, (v & 0x01) != 0);
				Registers.setNZ(c, r);
				return r;
			});

	public static final Operation ROL = Operation.modify("ROL", "Z,C,N",
			"Rotate Left - Moves all the bits one place to the left. Bit 0 is filled with the current carry and the old bit 7 becomes the new carry.",
			(c, v) -> {
				int r = ((v << 1) | (c.CFLAG.isSet() ? 1 : 0)) & 0xff;
				Registers.setCarry(c, (v & 0x80) != 0);
				Registers.setNZ(c, r);
				return r;
			});

	public static final Operation ROR = Operation.modify("ROR", "Z,C,N",
			"Rotate Right - Moves all the bits one place to the right. Bit 7 is filled with the current carry and the old bit 0 becomes the new carry.",
			(c, v) -> {
				int r = ((v & 0xff) >> 1) | (c.CFLAG.isSet() ? 0x80 : 0);
				Registers.setCarry(c, (v & 0x01) != 0);
				Registers.setNZ(c, r);
				return r;
			});

	// ---- Compares --------------------------------------------------------------
	public static final Operation CMP = Operation.read("CMP", "Z,C,N",
			"Z,C,N = A-M - Compares the accumulator with a memory value, setting the flags as if a subtraction had been carried out.",
			(c, v) -> compare(c, c.a.get(), v));

	public static final Operation CPX = Operation.read("CPX", "Z,C,N",
			"Z,C,N = X-M - Compares the X register with a memory value, setting the flags as if a subtraction had been carried out.",
			(c, v) -> compare(c, c.x.get(), v));

	public static final Operation CPY = Operation.read("CPY", "Z,C,N",
			"Z,C,N = Y-M - Compares the Y register with a memory value, setting the flags as if a subtraction had been carried out.",
			(c, v) -> compare(c, c.y.get(), v));

	/** C = register >= operand (unsigned), Z = equal, N = bit 7 of the 8-bit difference. V is unaffected. */
	private static void compare(com.mj.cpu001.CPU c, int register, int operand) {
		register &= 0xff;
		operand &= 0xff;
		Registers.setCarry(c, register >= operand);
		Registers.setNZ(c, register - operand);
	}

	// ---- Add and subtract with carry --------------------------------------------
	//
	// NMOS 6502 behavior, following Bruce Clark's "Decimal Mode" appendix
	// (http://www.6502.org/tutorials/decimal_mode.html). In decimal mode the accumulator
	// and carry use the BCD correction sequences, but N, V and Z come from different
	// intermediate values (ADC) or from the binary result (SBC).

	public static final Operation ADC = Operation.read("ADC", "Z,C,V,N",
			"A,Z,C,N,V = A+M+C - Adds the contents of a memory location to the accumulator together with the carry bit. "
					+ "Honors the decimal flag (NMOS behavior).",
			(c, m) -> addWithCarry(c, m));

	public static final Operation SBC = Operation.read("SBC", "Z,C,V,N",
			"A,Z,C,N,V = A-M-(1-C) - Subtracts the contents of a memory location from the accumulator together with the "
					+ "not of the carry bit. Honors the decimal flag (NMOS behavior).",
			(c, m) -> subtractWithBorrow(c, m));

	private static void setOverflow(com.mj.cpu001.CPU c, boolean on) {
		if (on) {
			c.OFLAG.set();
		} else {
			c.OFLAG.clear();
		}
	}

	private static void addWithCarry(com.mj.cpu001.CPU c, int m) {
		int a = c.a.get() & 0xff;
		m &= 0xff;
		int carry = c.CFLAG.isSet() ? 1 : 0;
		int binary = a + m + carry;

		if (!c.DFLAG.isSet()) {
			int result = binary & 0xff;
			Registers.setCarry(c, binary > 0xff);
			setOverflow(c, ((~(a ^ m)) & (a ^ binary) & 0x80) != 0);
			Registers.put(c.a, result);
			Registers.setNZ(c, result);
			return;
		}

		// decimal mode
		int low = (a & 0x0f) + (m & 0x0f) + carry;
		if (low >= 0x0a) {
			low = ((low + 0x06) & 0x0f) + 0x10;
		}
		int sum = (a & 0xf0) + (m & 0xf0) + low;
		// N and V come from the sum before the high-digit correction
		if ((sum & 0x80) != 0) {
			c.NFLAG.set();
		} else {
			c.NFLAG.clear();
		}
		setOverflow(c, ((~(a ^ m)) & (a ^ sum) & 0x80) != 0);
		// Z comes from the binary sum, not the BCD result (an NMOS quirk)
		if ((binary & 0xff) == 0) {
			c.ZFLAG.set();
		} else {
			c.ZFLAG.clear();
		}
		if (sum >= 0xa0) {
			sum += 0x60;
		}
		Registers.setCarry(c, sum > 0xff);
		Registers.put(c.a, sum & 0xff);
	}

	private static void subtractWithBorrow(com.mj.cpu001.CPU c, int m) {
		int a = c.a.get() & 0xff;
		m &= 0xff;
		int carry = c.CFLAG.isSet() ? 1 : 0;
		int binary = a - m - (1 - carry);
		int binaryResult = binary & 0xff;

		// all flags are the binary ones, in both modes
		Registers.setCarry(c, binary >= 0);
		setOverflow(c, ((a ^ m) & (a ^ binary) & 0x80) != 0);
		Registers.setNZ(c, binaryResult);

		if (!c.DFLAG.isSet()) {
			Registers.put(c.a, binaryResult);
			return;
		}

		int low = (a & 0x0f) - (m & 0x0f) + carry - 1;
		if (low < 0) {
			low = ((low - 0x06) & 0x0f) - 0x10;
		}
		int result = (a & 0xf0) - (m & 0xf0) + low;
		if (result < 0) {
			result -= 0x60;
		}
		Registers.put(c.a, result & 0xff);
	}
}
