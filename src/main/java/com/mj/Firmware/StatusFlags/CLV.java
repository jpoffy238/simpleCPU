package com.mj.Firmware.StatusFlags;

import com.mj.Firmware.Framework.Instruction;
import com.mj.cpu001.CPU;

public class CLV extends Instruction {
	public CLV() {
		super((byte)0xb8);
		setProperty(KEY_MNEMONIC, "CLV");
		setProperty(KEY_ADDRESSING_MODE, VALUE_ADDM_IMP);
		setProperty(KEY_OPCODE, "0xb8");
		setProperty(KEY_INSTRUCTION_SIZE, "1");
		setProperty(KEY_CYCLES, "2");
		setProperty(KEY_WEB,"http://6502.org/tutorials/6502opcodes.html#TAX" );
		setProperty(KEY_DESCRIPTION ,"X = A  Stores the contents of the accumulator X.");
	}
	public void exeute(CPU c) {
		c.OFLAG.clear();
	}
}
