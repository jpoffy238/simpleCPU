package Firmeware;

import Registers.registerFlags;
import cpu001.CPU;

public class LDAZX extends Instruction {
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
