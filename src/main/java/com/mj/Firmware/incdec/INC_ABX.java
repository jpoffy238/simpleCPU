package com.mj.Firmware.incdec;

import com.mj.Firmware.Framework.Instruction;
import com.mj.cpu001.CPU;
import com.mj.exceptions.DeviceUnavailable;
import com.mj.exceptions.ROException;
import com.mj.exceptions.illegalAddressException;

public class INC_ABX extends Instruction {

	public INC_ABX() {
		super((byte)0xfe);
	}
	public void exeute(CPU c) throws illegalAddressException, DeviceUnavailable, ROException {
		int zeroPageAddress = getAbsoluteAddressX(c);
		
		int data = c.bus.read(zeroPageAddress);
		data++;
		data &= 0x00ff;
		if (data == 0) {
			handleZException(c);
		}
		if ((data & 0x0080) != 0) {
			handleNException(c);
		}
		c.bus.write(zeroPageAddress, (byte)data);
		c.pc+=2;
		
	}
}
