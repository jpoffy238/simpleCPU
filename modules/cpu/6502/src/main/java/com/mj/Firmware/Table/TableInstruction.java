package com.mj.Firmware.Table;

import com.mj.Firmware.Framework.Instruction;
import com.mj.cpu001.CPU;
import com.mj.exceptions.DeviceUnavailable;
import com.mj.exceptions.ROException;
import com.mj.exceptions.illegalAddressException;

/**
 * A single generic instruction implementation: {@code opcode = operation x
 * addressing mode}. Operand fetch, write-back and the PC advance are done here
 * once for every table-driven instruction.
 */
public class TableInstruction extends Instruction {
	private final Operation operation;
	private final AddrMode mode;
	private final int cycles;

	public TableInstruction(int opcode, Operation operation, AddrMode mode, int cycles) {
		super((byte) opcode);
		this.operation = operation;
		this.mode = mode;
		this.cycles = cycles;
		setProperty(KEY_MNEMONIC, operation.mnemonic());
		setProperty(KEY_ADDRESSING_MODE, mode.label());
		setProperty(KEY_OPCODE, String.format("0x%02x", opcode & 0xff));
		setProperty(KEY_INSTRUCTION_SIZE, Integer.toString(mode.size()));
		setProperty(KEY_CYCLES, Integer.toString(cycles) + (mode.hasPageCrossPenalty() && operation.kind() == Operation.Kind.READ ? "+" : ""));
		setProperty(KEY_FLAGS_EFFECTED, operation.flags());
		setProperty(KEY_WEB, "http://6502.org/tutorials/6502opcodes.html#" + operation.mnemonic());
		setProperty(KEY_DESCRIPTION, operation.description());
	}

	public Operation operation() {
		return operation;
	}

	public AddrMode mode() {
		return mode;
	}

	/** Base cycle count (before any page-crossing penalty). */
	public int cycles() {
		return cycles;
	}

	@Override
	public void exeute(CPU c) throws illegalAddressException, DeviceUnavailable, ROException {
		switch (operation.kind()) {
		case READ: {
			int value = c.bus.read(mode.address(c)) & 0xff;
			operation.readOp().apply(c, value);
			break;
		}
		case MODIFY: {
			if (mode == AddrMode.ACCUMULATOR) {
				Registers.put(c.a, operation.modifyOp().apply(c, c.a.get()));
			} else {
				int address = mode.address(c);
				int result = operation.modifyOp().apply(c, c.bus.read(address) & 0xff);
				c.bus.write(address, (byte) result);
			}
			break;
		}
		case STORE: {
			c.bus.write(mode.address(c), (byte) operation.storeOp().value(c));
			break;
		}
		}
		c.pc += mode.operandBytes();
	}
}
