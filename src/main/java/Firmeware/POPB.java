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
		try {
			c.sp.inc();
		} catch (cflagException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	

	}

	public void setFlags(CPU u) {
		// TODO Auto-generated method stub

	}

}
