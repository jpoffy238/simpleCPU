package Firmeware.ExecutionFlow;

import Firmeware.Framework.Instruction;
import cpu001.CPU;

public class JRZ extends Instruction {

	public JRZ() {
		super((byte)(0x00));
	}
	public void exeute(CPU c) {
		// TODO Auto-generated method stub
		if (c.flags.ZFLAG.isSet()) {
			byte offset = c.memory.read(c.pc);
			c.pc = (int)(c.pc+offset);
		}
	}

	public void setFlags(CPU u) {
		// TODO Auto-generated method stub

	}

}
