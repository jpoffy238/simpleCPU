package Firmeware;

import cpu001.CPU;
import exceptions.cflagException;

public class POPB extends Instruction {

	public POPB() {
		
	}
	public void exeute(CPU c) {
		// TODO Auto-generated method stub
			int madd = (int)(c.sp.get()) ;
			
		c.b.set(c.memory.read(madd));
		
			c.sp.inc();
		
		
	

	}

	public void setFlags(CPU u) {
		// TODO Auto-generated method stub

	}

}
