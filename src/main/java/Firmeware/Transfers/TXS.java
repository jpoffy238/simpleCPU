package Firmeware.Transfers;

import Firmeware.Framework.Instruction;
import cpu001.CPU;

public class TXS extends Instruction {
	public TXS() {
		super((byte)0x9a);
		setProperty(KEY_MNEMONIC, "TXS");
		setProperty(KEY_ADDRESSING_MODE, VALUE_ADDM_IMP);
		setProperty(KEY_OPCODE, "0x9a");
		setProperty(KEY_INSTRUCTION_SIZE, "1");
		setProperty(KEY_CYCLES, "2");
		setProperty(KEY_WEB,"http://6502.org/tutorials/6502opcodes.html#TYA" );
		setProperty(KEY_DESCRIPTION ,"X = A  Stores the contents of the accumulator X.");
	}
	public void exeute(CPU c) {
		c.sp = (c.x.get()&0xff + 0x0100);
	}
}
