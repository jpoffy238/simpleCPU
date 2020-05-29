package Firmeware.Load;

import Firmeware.Framework.Instruction;
import Registers.registerFlags;
import cpu001.CPU;


public class LDX extends Instruction {
	//TODO Setup correct opcode - using default
public LDX() {
	super((byte)0x00);
}
	public void exeute(CPU c) {
		// TODO Auto-generated method stub
		byte m = c.memory.read(c.pc);
		c.x.set(m);
	
		c.pc = (++c.pc);
	}
}
