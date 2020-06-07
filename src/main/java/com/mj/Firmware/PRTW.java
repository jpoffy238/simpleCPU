package com.mj.Firmware;

import com.mj.Devices.Ports;
import com.mj.Devices.PortsAccess;
import com.mj.Firmware.Framework.Instruction;
import com.mj.cpu001.CPU;

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
