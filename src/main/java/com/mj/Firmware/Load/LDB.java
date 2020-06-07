package com.mj.Firmware.Load;

import com.mj.Firmware.Framework.Instruction;
import com.mj.cpu001.CPU;
import com.mj.exceptions.DeviceUnavailable;
import com.mj.exceptions.illegalAddressException;
import com.mj.exceptions.nflagException;
import com.mj.exceptions.zflagException;

public class LDB extends Instruction {
	public LDB () {
	 super((byte)0x3E);
	 //  NON-6502 Instruction 
	}
	public void exeute(CPU c) throws illegalAddressException, DeviceUnavailable {
	
		byte m = c.memory.read(c.pc);
		try {
			c.b.set(m);
		} catch (zflagException e) {
			handleZException(c);
		} catch (nflagException e) {
			handleNException(c);
		}
		c.pc++;
	}

}
