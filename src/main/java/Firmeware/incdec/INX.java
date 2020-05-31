package Firmeware.incdec;

import Firmeware.Framework.Instruction;
import cpu001.CPU;
import exceptions.DeviceUnavailable;
import exceptions.illegalAddressException;
import exceptions.nflagException;
import exceptions.zflagException;

public class INX extends Instruction {

	public INX() {
		super((byte)0xe8);
	}
	public void exeute(CPU c) throws illegalAddressException, DeviceUnavailable {
		try {
			c.x.inc();
		} catch (nflagException e) {
			handleNException(c);
		} catch (zflagException e) {
			// TODO Auto-generated catch block
			handleZException(c);
		}
	
		
	}
}
