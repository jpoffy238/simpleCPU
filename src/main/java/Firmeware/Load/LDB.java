package Firmeware.Load;

import Firmeware.Framework.Instruction;
import cpu001.CPU;
import exceptions.DeviceUnavailable;
import exceptions.illegalAddressException;
import exceptions.nflagException;
import exceptions.zflagException;

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