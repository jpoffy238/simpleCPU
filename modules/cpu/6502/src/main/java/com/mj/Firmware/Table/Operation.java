package com.mj.Firmware.Table;

import com.mj.cpu001.CPU;

/**
 * What an instruction does with its operand, independent of addressing mode.
 * There are three shapes: read an operand, read-modify-write it, or store a
 * register to it.
 */
public final class Operation {
	public enum Kind {
		/** Consumes the operand value (LDA, AND, ADC, CMP, ...). */
		READ,
		/** Transforms the operand and writes it back (ASL, INC, ...). */
		MODIFY,
		/** Writes a register to the operand address (STA, STX, STY). */
		STORE,
		/** Acts on registers or flags only, with no operand (INX, DEY, TAX, CLC, ...). */
		IMPLIED
	}

	@FunctionalInterface
	public interface ReadOp {
		void apply(CPU c, int value);
	}

	@FunctionalInterface
	public interface ModifyOp {
		int apply(CPU c, int value);
	}

	@FunctionalInterface
	public interface StoreOp {
		int value(CPU c);
	}

	@FunctionalInterface
	public interface ImpliedOp {
		void apply(CPU c);
	}

	private final Kind kind;
	private final String mnemonic;
	private final String flags;
	private final String description;
	private final ReadOp readOp;
	private final ModifyOp modifyOp;
	private final StoreOp storeOp;
	private final ImpliedOp impliedOp;

	private Operation(Kind kind, String mnemonic, String flags, String description, ReadOp r, ModifyOp m, StoreOp s,
			ImpliedOp i) {
		this.kind = kind;
		this.mnemonic = mnemonic;
		this.flags = flags;
		this.description = description;
		this.readOp = r;
		this.modifyOp = m;
		this.storeOp = s;
		this.impliedOp = i;
	}

	public static Operation read(String mnemonic, String flags, String description, ReadOp op) {
		return new Operation(Kind.READ, mnemonic, flags, description, op, null, null, null);
	}

	public static Operation modify(String mnemonic, String flags, String description, ModifyOp op) {
		return new Operation(Kind.MODIFY, mnemonic, flags, description, null, op, null, null);
	}

	public static Operation store(String mnemonic, String flags, String description, StoreOp op) {
		return new Operation(Kind.STORE, mnemonic, flags, description, null, null, op, null);
	}

	public static Operation implied(String mnemonic, String flags, String description, ImpliedOp op) {
		return new Operation(Kind.IMPLIED, mnemonic, flags, description, null, null, null, op);
	}

	public Kind kind() {
		return kind;
	}

	public String mnemonic() {
		return mnemonic;
	}

	public String flags() {
		return flags;
	}

	public String description() {
		return description;
	}

	ReadOp readOp() {
		return readOp;
	}

	ModifyOp modifyOp() {
		return modifyOp;
	}

	ImpliedOp impliedOp() {
		return impliedOp;
	}

	StoreOp storeOp() {
		return storeOp;
	}
}
