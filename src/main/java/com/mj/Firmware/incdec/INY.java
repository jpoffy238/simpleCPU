package com.mj.Firmware.incdec;

import com.mj.Firmware.Framework.Instruction;
import com.mj.cpu001.CPU;
import com.mj.exceptions.DeviceUnavailable;
import com.mj.exceptions.illegalAddressException;
import com.mj.exceptions.nflagException;
import com.mj.exceptions.zflagException;

public class INY extends Instruction {

	public INY() {
		super((byte)0xc8);
	}
	public void exeute(CPU c) throws illegalAddressException, DeviceUnavailable {
		try {
			c.ZFLAG.clear();
			c.NFLAG.clear();
			c.y.inc();
		} catch (nflagException e) {
			handleNException(c);
		} catch (zflagException e) {
			// TODO Auto-generated catch block
			handleZException(c);
		}
	
		
	}
}
