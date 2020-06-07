package com.mj.Firmware.Framework;

import com.mj.cpu001.CPU;
import com.mj.exceptions.DeviceUnavailable;
import com.mj.exceptions.illegalAddressException;

public class MIH extends Instruction {

	public MIH() {
		super((byte) (0x3E));
	}

	public void exeute(CPU c) throws illegalAddressException, DeviceUnavailable {
		// TODO Auto-generated method stub
		int interuptHandlerAddress = 0; 
		if (c.getNMInterruptFired()) {
			interuptHandlerAddress = getNMInterruptHandlerAddress(c);
		} else {
			if (c.getInterruptFired()) {
				interuptHandlerAddress = getInterruptHandlerAddress(c);
			} else {
				if (c.getPowerOnResetFired()) {
					interuptHandlerAddress = getResetHandlerAddress(c);
				}
			}
		}
		if (interuptHandlerAddress != 0) {

			int upper = (((c.pc) & 0xff00) >> 8);
			int lower = (((c.pc) & 0x00ff));

			// push on stack
			c.sp--;
			c.memory.write(c.sp, (byte) (upper & 0xff));
			c.sp--;
			c.memory.write(c.sp, (byte) (lower & 0xff));
			c.sp--;
			c.memory.write(c.sp, psr(c));

			c.pc = interuptHandlerAddress;
		}
	}

}
