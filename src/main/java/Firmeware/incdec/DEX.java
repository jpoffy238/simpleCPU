package Firmeware.incdec;

import Firmeware.Framework.Instruction;
import cpu001.CPU;
import exceptions.DeviceUnavailable;
import exceptions.illegalAddressException;
import exceptions.nflagException;
import exceptions.zflagException;

public class DEX extends Instruction {

	public DEX() {
		super((byte)0xca);
	}
	public void exeute(CPU c) throws illegalAddressException, DeviceUnavailable {
		try {
			c.x.dec();
		} catch (nflagException e) {
			handleNException(c);
		} catch (zflagException e) {
			// TODO Auto-generated catch block
			handleZException(c);
		}
	
		
	}
}
