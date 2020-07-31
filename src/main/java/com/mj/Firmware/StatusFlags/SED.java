package com.mj.Firmware.StatusFlags;

import com.mj.Firmware.Framework.Instruction;
import com.mj.cpu001.CPU;

public class SED extends Instruction {
	public SED() {
		super((byte)0xf8);
		setProperty(KEY_MNEMONIC, "SED");
		setProperty(KEY_ADDRESSING_MODE, VALUE_ADDM_IMP);
		setProperty(KEY_OPCODE, "0xf8");
		setProperty(KEY_INSTRUCTION_SIZE, "1");
		setProperty(KEY_CYCLES, "2");
		setProperty(KEY_WEB,"http://6502.org/tutorials/6502opcodes.html#SED" );
		setProperty(KEY_DESCRIPTION ,"Set Decimal Flag.");
	}
	public void exeute(CPU c) {
		c.DFLAG.set();
	}
}
