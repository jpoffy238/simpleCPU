package Firmeware.incdec;

import Firmeware.Framework.Instruction;
import cpu001.CPU;
import exceptions.DeviceUnavailable;
import exceptions.illegalAddressException;
import exceptions.nflagException;
import exceptions.zflagException;

public class INY extends Instruction {

	public INY() {
		super((byte)0xc8);
	}
	public void exeute(CPU c) throws illegalAddressException, DeviceUnavailable {
		try {
			c.y.inc();
		} catch (nflagException e) {
			handleNException(c);
		} catch (zflagException e) {
			// TODO Auto-generated catch block
			handleZException(c);
		}
	
		
	}
}
