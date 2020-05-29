package Firmeware.Math;

import Firmeware.Framework.Instruction;
import Registers.registerFlags;
import cpu001.CPU;

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
			registerFlags.CFLAG.isSet();
		}
		c.clockState++;
		c.a.set((byte)value & 0xff);

	}

	public void setFlags(CPU u) {
		// TODO Auto-generated method stub

	}

}
