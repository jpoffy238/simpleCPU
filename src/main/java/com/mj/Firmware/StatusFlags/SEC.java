package com.mj.Firmware.StatusFlags;

import com.mj.Firmware.Framework.Instruction;
import com.mj.cpu001.CPU;

public class SEC extends Instruction {
	public SEC() {
		super((byte)0x38);
		setProperty(KEY_MNEMONIC, "SEC");
		setProperty(KEY_ADDRESSING_MODE, VALUE_ADDM_IMP);
		setProperty(KEY_OPCODE, "0x38");
		setProperty(KEY_INSTRUCTION_SIZE, "1");
		setProperty(KEY_CYCLES, "2");
		setProperty(KEY_WEB,"http://6502.org/tutorials/6502opcodes.html#SEC" );
		setProperty(KEY_DESCRIPTION ,"Set Carry Flag.");
	}
	public void exeute(CPU c) {
		c.CFLAG.set();
	}
}
