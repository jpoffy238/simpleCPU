package com.mj.Firmware.Transfers;

import com.mj.Firmware.Framework.Instruction;
import com.mj.cpu001.CPU;
import com.mj.exceptions.nflagException;
import com.mj.exceptions.zflagException;

public class TXA extends Instruction {
	public TXA() {
		super((byte)0x8a);
		setProperty(KEY_MNEMONIC, "TXA");
		setProperty(KEY_ADDRESSING_MODE, VALUE_ADDM_IMP);
		setProperty(KEY_OPCODE, "0xaa");
		setProperty(KEY_INSTRUCTION_SIZE, "a");
		setProperty(KEY_CYCLES, "2");
		setProperty(KEY_WEB,"http://6502.org/tutorials/6502opcodes.html#TAX" );
		setProperty(KEY_DESCRIPTION ,"X = A  Stores the contents of the accumulator X.");
	}
	public void exeute(CPU c) {
		try {
			c.a.set(c.x.get());
		} catch (zflagException e) {
			handleZException(c);
		} catch (nflagException e) {
			handleNException(c);
		}
	}
}
