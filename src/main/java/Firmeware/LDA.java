package Firmeware;

import Registers.registerFlags;
import cpu001.CPU;

public class LDA extends Instruction {
// Load from Immediate on byte following
// instructions.
	public LDA () {
		
	}
	public void exeute(CPU c) {
		// TODO Auto-generated method stub
		byte m = c.memory.read(c.pc);
		c.a.set(m);
		if ( m == 0) {
			registerFlags.ZFLAG.set();
		}
		c.pc = (++c.pc)&0x00ff;
	}

	public void setFlags(CPU c) {
		// TODO Auto-generated method stub

	}

}
