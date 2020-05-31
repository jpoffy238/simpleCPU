package Firmeware.Stack;

import Firmeware.Framework.Instruction;
import cpu001.CPU;
import exceptions.DeviceUnavailable;
import exceptions.illegalAddressException;

public class PLP extends Instruction {

	public PLP () {
		super((byte)0x28);
	}
	public void exeute(CPU c) throws illegalAddressException, DeviceUnavailable {
		// TODO Auto-generated method stub
		
		
		
		
		byte data;
		
		data = c.memory.read(c.sp);
		c.sp++;
		setFlags(c, data);
		
	}


}
