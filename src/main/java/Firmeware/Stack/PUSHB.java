package Firmeware.Stack;

import Firmeware.Framework.Instruction;
import cpu001.CPU;
import exceptions.DeviceUnavailable;
import exceptions.illegalAddressException;

public class PUSHB extends Instruction {

	public PUSHB () {
		super((byte)0x32);
	}
	public void exeute(CPU c) throws illegalAddressException, DeviceUnavailable {
		// TODO Auto-generated method stub
		
		
		
		c.sp--;
		byte data = (byte)(c.b.get() & 0xff);
		
		c.memory.write(c.sp, data);
		
	}

	

}