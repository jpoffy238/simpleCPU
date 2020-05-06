package Firmeware;

import cpu001.CPU;

public class CMP extends Instruction {

	public CMP () {
	
	}
	public void exeute(CPU c) {
		// TODO Auto-generated method stub
		int a = c.a.get();
		int b = c.b.get();
		int result = a - b;
		if (result == 0) {
			c.flags.ZFLAG.isSet();
		} else {
			if (result < 0) {
				c.flags.NFLAG.set();
			}  else {
				c.flags.NFLAG.clear();
			}
		}
	}

	public void setFlags(CPU u) {
		// TODO Auto-generated method stub

	}

}
