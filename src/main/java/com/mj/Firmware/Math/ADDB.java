package com.mj.Firmware.Math;

import com.mj.Firmware.Framework.Instruction;
import com.mj.cpu001.CPU;
import com.mj.exceptions.nflagException;
import com.mj.exceptions.zflagException;

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
