package Firmeware;

import Devices.Ports;
import Devices.PortsAccess;
import Firmeware.Framework.Instruction;
import cpu001.CPU;

public class PRTW extends Instruction {
	// Writes contents of Register A to port referenced by Register B 
	// 
	public PRTW () {
	super((byte)(0x00));
	}
	PortsAccess port =  new Ports();
	public void exeute(CPU c) {
		// TODO Auto-generated method stub
		
		port.write((byte)(c.b.get()  &0xff),  (byte)(c.a.get() & 0xff));
		
	}

}
