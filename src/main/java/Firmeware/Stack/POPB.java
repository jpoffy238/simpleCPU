package Firmeware.Stack;

import Firmeware.Framework.Instruction;
import cpu001.CPU;
import exceptions.DeviceUnavailable;
import exceptions.cflagException;
import exceptions.illegalAddressException;
import exceptions.nflagException;
import exceptions.zflagException;

public class POPB extends Instruction {

	public POPB() {
		super((byte)(0x31));
	}
	public void exeute(CPU c) throws  illegalAddressException, DeviceUnavailable {
		// TODO Auto-generated method stub
			int madd = (int)(c.sp) ;
			int value = c.memory.read(madd);
			
		try {
			c.b.set(value & 0x00ff);
		} catch (zflagException e) {
			c.ZFLAG.set();
		} catch (nflagException e) {
			// TODO Auto-generated catch block
			c.NFLAG.set();
		}
		
			c.sp++;
		
		
	

	}

	public void setFlags(CPU u) {
		// TODO Auto-generated method stub

	}

}
