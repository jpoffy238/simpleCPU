package Firmeware.Load;

import Firmeware.Framework.Instruction;
import Registers.registerFlags;
import cpu001.CPU;

public class LDB extends Instruction {
	public LDB () {
	 super((byte)0x3E);
	}
	public void exeute(CPU c) {
		// TODO Auto-generated method stub
		byte m = c.memory.read(c.pc);
		c.b.set(m);
		if ( m == 0) {
			registerFlags.ZFLAG.set();
		}
		c.pc = (++c.pc);
	}

	public void setFlags(CPU u) {
		// TODO Auto-generated method stub

	}

}
