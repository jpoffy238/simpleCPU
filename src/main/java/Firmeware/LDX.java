package Firmeware;

import Registers.registerFlags;
import cpu001.CPU;

public class LDX extends Instruction {
	public void exeute(CPU c) {
		// TODO Auto-generated method stub
		byte m = c.memory.read(c.pc);
		c.x.set(m);
	
		c.pc = (++c.pc);
	}
}
