package Firmeware.Transfers;

import Firmeware.Framework.Instruction;
import cpu001.CPU;
import exceptions.nflagException;
import exceptions.zflagException;

public class TYA extends Instruction {
	public TYA() {
		super((byte)0x98);
		setProperty(KEY_MNEMONIC, "TAY");
		setProperty(KEY_ADDRESSING_MODE, VALUE_ADDM_IMP);
		setProperty(KEY_OPCODE, "0x98");
		setProperty(KEY_INSTRUCTION_SIZE, "a");
		setProperty(KEY_CYCLES, "2");
		setProperty(KEY_WEB,"http://6502.org/tutorials/6502opcodes.html#TAX" );
		setProperty(KEY_DESCRIPTION ,"X = A  Stores the contents of the accumulator X.");
	}
	public void exeute(CPU c) {
		try {
			c.a.set(c.y.get());
		} catch (zflagException e) {
			handleZException(c);
		} catch (nflagException e) {
			handleNException(c);
		}
	}
}
