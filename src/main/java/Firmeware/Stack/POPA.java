package Firmeware.Stack;

import Firmeware.Framework.Instruction;
import cpu001.CPU;
import exceptions.cflagException;

public class POPA extends Instruction {

	public POPA() {
	
			super((byte)(0x00));
		
	}
	public void exeute(CPU c) {
		// TODO Auto-generated method stub
			int madd = (int)(c.sp.get()) ;
			
		c.a.set(c.memory.read(madd));
		
			c.sp.inc();
		
		
	

	}

	public void setFlags(CPU u) {
		// TODO Auto-generated method stub

	}

}
