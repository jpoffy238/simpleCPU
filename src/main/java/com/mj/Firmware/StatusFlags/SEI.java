package com.mj.Firmware.StatusFlags;

import com.mj.Firmware.Framework.Instruction;
import com.mj.cpu001.CPU;

public class SEI extends Instruction {
	public SEI() {
		super((byte)0x78);
		setProperty(KEY_MNEMONIC, "SEI");
		setProperty(KEY_ADDRESSING_MODE, VALUE_ADDM_IMP);
		setProperty(KEY_OPCODE, "0x78");
		setProperty(KEY_INSTRUCTION_SIZE, "1");
		setProperty(KEY_CYCLES, "2");
		setProperty(KEY_WEB,"http://6502.org/tutorials/6502opcodes.html#SEI" );
		setProperty(KEY_DESCRIPTION ,"Set Interrupt flag");
	}
	public void exeute(CPU c) {
		c.IFLAG.set();
	}
}
