package com.mj.Firmware.Stack;

import com.mj.Firmware.Framework.Instruction;
import com.mj.cpu001.CPU;
import com.mj.exceptions.DeviceUnavailable;
import com.mj.exceptions.ROException;
import com.mj.exceptions.illegalAddressException;

public class PHA extends Instruction {

	public PHA () {
		super((byte)0x48);
	}
	public void exeute(CPU c) throws illegalAddressException, DeviceUnavailable, ROException {
		// TODO Auto-generated method stub
		
		
		
		c.sp--;
		byte data = (byte)(c.a.get() & 0xff);
		
		c.bus.write(c.sp, data);
			
	}


}
