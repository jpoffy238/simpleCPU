package Firmeware.Transfers;

import Firmeware.Framework.Instruction;
import cpu001.CPU;
import exceptions.nflagException;
import exceptions.zflagException;

public class TSX extends Instruction {
	public TSX() {
		super((byte)0xba);
		setProperty(KEY_MNEMONIC, "TSX");
		setProperty(KEY_ADDRESSING_MODE, VALUE_ADDM_IMP);
		setProperty(KEY_OPCODE, "0xba");
		setProperty(KEY_INSTRUCTION_SIZE, "1");
		setProperty(KEY_CYCLES, "2");
		setProperty(KEY_WEB,"http://6502.org/tutorials/6502opcodes.html#TYA" );
		setProperty(KEY_DESCRIPTION ,"X = A  Stores the contents of the accumulator X.");
	}
	public void exeute(CPU c) {
		try {
			c.x.set(c.sp&0xff);
		} catch (zflagException e) {
			handleZException(c);
		} catch (nflagException e) {
			handleNException(c);
		}
	}
}
