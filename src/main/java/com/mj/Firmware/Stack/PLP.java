package com.mj.Firmware.Stack;

import com.mj.Firmware.Framework.Instruction;
import com.mj.cpu001.CPU;
import com.mj.exceptions.DeviceUnavailable;
import com.mj.exceptions.illegalAddressException;

public class PLP extends Instruction {

	public PLP () {
		super((byte)0x28);
	}
	public void exeute(CPU c) throws illegalAddressException, DeviceUnavailable {
		// TODO Auto-generated method stub
		
		
		
		
		byte data;
		
		data = c.bus.read(c.sp);
		c.sp++;
		setFlags(c, data);
		
	}


}
