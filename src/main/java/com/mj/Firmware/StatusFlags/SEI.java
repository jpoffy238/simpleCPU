package com.mj.Firmware.StatusFlags;

import com.mj.Firmware.Framework.Instruction;
import com.mj.cpu001.CPU;
import com.mj.exceptions.nflagException;
import com.mj.exceptions.zflagException;

public class SEI extends Instruction {
	public SEI() {
		super((byte)0x78);
		setProperty(KEY_MNEMONIC, "SEI");
		setProperty(KEY_ADDRESSING_MODE, VALUE_ADDM_IMP);
		setProperty(KEY_OPCODE, "0x78");
		setProperty(KEY_INSTRUCTION_SIZE, "1");
		setProperty(KEY_CYCLES, "2");
		setProperty(KEY_WEB,"http://6502.org/tutorials/6502opcodes.html#TAX" );
		setProperty(KEY_DESCRIPTION ,"X = A  Stores the contents of the accumulator X.");
	}
	public void exeute(CPU c) {
		c.IFLAG.set();
	}
}
