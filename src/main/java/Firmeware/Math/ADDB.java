package Firmeware.Math;

import Firmeware.Framework.Instruction;
import Registers.registerFlags;
import cpu001.CPU;
import exceptions.nflagException;
import exceptions.zflagException;

public class ADDB extends Instruction  {

public ADDB () {	
	super((byte)0x30);
}
	public void exeute(CPU c) {
		// TODO Auto-generated method stub
		int a = c.a.get();
		int b = c.b.get();
		c.clockState++;
		int value = a + b;
		if (value > 255 ) {
			value = value & 0xff;
			c.CFLAG.set();
		}
		c.clockState++;
		try {
			c.a.set((byte)value & 0xff);
		} catch (zflagException e) {
			// TODO Auto-generated catch block
			c.ZFLAG.set();
			c.NFLAG.clear();
		} catch (nflagException e) {
			// TODO Auto-generated catch block
			c.NFLAG.set();
			c.ZFLAG.clear();
		}

	}



}
