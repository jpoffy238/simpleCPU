package Firmeware.Load;

import Firmeware.Framework.Instruction;
import Registers.registerFlags;
import cpu001.CPU;

public class LDAZX extends Instruction {
	// Zero Page reference (X)
	public LDAZX() {
		super ((byte)0xa9);
	}
	public void exeute(CPU c) {
		// TODO Auto-generated method stub
		byte m = c.memory.read(c.x.get() &0x00ff);
		c.a.set(m);
		if ( m == 0) {
			registerFlags.ZFLAG.set();
		}
		c.pc = (++c.pc);
	}
}
