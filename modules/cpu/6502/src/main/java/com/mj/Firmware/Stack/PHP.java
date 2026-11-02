package com.mj.Firmware.Stack;

import com.mj.Firmware.Framework.Instruction;
import com.mj.cpu001.CPU;
import com.mj.exceptions.DeviceUnavailable;
import com.mj.exceptions.ROException;
import com.mj.exceptions.illegalAddressException;

public class PHP extends Instruction {

	public PHP () {
		super((byte)0x08);
	}
	public void exeute(CPU c) throws illegalAddressException, DeviceUnavailable, ROException {
		// TODO Auto-generated method stub
		
		
		
		c.push(psr(c, true));   // PHP pushes B set
			
	}


}
