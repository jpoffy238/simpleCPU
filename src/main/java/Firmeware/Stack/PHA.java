package Firmeware.Stack;

import Firmeware.Framework.Instruction;
import cpu001.CPU;
import exceptions.DeviceUnavailable;
import exceptions.illegalAddressException;

public class PHA extends Instruction {

	public PHA () {
		super((byte)0x48);
	}
	public void exeute(CPU c) throws illegalAddressException, DeviceUnavailable {
		// TODO Auto-generated method stub
		
		
		
		c.sp--;
		byte data = (byte)(c.a.get() & 0xff);
		
		c.memory.write(c.sp, data);
			
	}


}
