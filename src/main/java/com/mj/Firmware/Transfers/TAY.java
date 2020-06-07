package com.mj.Firmware.Transfers;

import com.mj.Firmware.Framework.Instruction;
import com.mj.cpu001.CPU;
import com.mj.exceptions.nflagException;
import com.mj.exceptions.zflagException;

public class TAY extends Instruction {
	public TAY() {
		super((byte)0xa8);
		setProperty(KEY_MNEMONIC, "TAY");
		setProperty(KEY_ADDRESSING_MODE, VALUE_ADDM_IMP);
		setProperty(KEY_OPCODE, "0xa8");
		setProperty(KEY_INSTRUCTION_SIZE, "a");
		setProperty(KEY_CYCLES, "2");
		setProperty(KEY_WEB,"http://6502.org/tutorials/6502opcodes.html#TAY" );
		setProperty(KEY_DESCRIPTION ,"X = A  Stores the contents of the accumulator X.");
	}
	public void exeute(CPU c) {
		try {
			c.y.set(c.a.get());
		} catch (zflagException e) {
			handleZException(c);
		} catch (nflagException e) {
			handleNException(c);
		}
	}
}
