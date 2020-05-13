package Firmeware;

import cpu001.CPU;

public class ADCI extends Instruction {
	public void exeute(CPU c) {
		// TODO Auto-generated method stub
		int m = c.memory.read(c.pc);
		c.pc = (++c.pc);
		int a = c.a.get();
		boolean anegative = (a & 0x80) > 0;
		boolean  mnegative  = (m & 0x80) > 0;
		
		int carryValue  = 0;
		if (c.flags.CFLAG.isSet() ) {
			carryValue = 1;
		}
		
		int result = m + c.a.get() + carryValue;
		if (result > 255 ) {
			c.flags.CFLAG.set();
		} else {
			if ((result & 0x80) != 0) {
				c.flags.OVFLAG.set();
			}
		}
		
			
		c.a.set(result & 0xff);
		if ( !anegative && !mnegative ) {
			c.flags.NFLAG.clear();
		}
		
	}
}
