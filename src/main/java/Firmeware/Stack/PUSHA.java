package Firmeware.Stack;

import Firmeware.Framework.Instruction;
import cpu001.CPU;

public class PUSHA extends Instruction {

	public PUSHA () {
		super((byte)0x00);
	}
	public void exeute(CPU c) {
		// TODO Auto-generated method stub
		
		
		int  sp = c.sp.get();
		sp--;
		byte data = (byte)(c.a.get() & 0xff);
		
		c.memory.write(sp, data);
		c.sp.set(sp);		
	}

	public void setFlags(CPU u) {
		// TODO Auto-generated method stub

	}

}
