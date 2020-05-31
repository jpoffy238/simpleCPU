package Firmeware.Stack;

import Firmeware.Framework.Instruction;
import cpu001.CPU;
import exceptions.DeviceUnavailable;
import exceptions.illegalAddressException;

public class PHP extends Instruction {

	public PHP () {
		super((byte)0x08);
	}
	public void exeute(CPU c) throws illegalAddressException, DeviceUnavailable {
		// TODO Auto-generated method stub
		
		
		
		c.sp--;
		byte data = (byte)( psr(c)& 0xff);
		
		c.memory.write(c.sp, data);
			
	}


}
