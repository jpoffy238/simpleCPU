package Firmeware.incdec;

import Firmeware.Framework.Instruction;
import cpu001.CPU;
import exceptions.DeviceUnavailable;
import exceptions.illegalAddressException;

public class DEC_ABS extends Instruction {

	public DEC_ABS() {
		super((byte)0xce);
	}
	public void exeute(CPU c) throws illegalAddressException, DeviceUnavailable {
		int zeroPageAddress = getAbsoluteAddress(c);
		
		int data = c.memory.read(zeroPageAddress);
		data--;
		data &= 0x00ff;
		if (data == 0) {
			handleZException(c);
		}
		if ((data & 0x0080) != 0) {
			handleNException(c);
		}
		c.memory.write(zeroPageAddress, (byte)data);
		c.pc+=2;
		
	}
}
