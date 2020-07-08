package com.mj.Firmware.Framework;

import com.mj.cpu001.CPU;
import com.mj.Devices.PBus;
import com.mj.exceptions.DeviceUnavailable;
import com.mj.exceptions.ROException;
import com.mj.exceptions.illegalAddressException;

public class MIH extends Instruction {

	public MIH() {
		super((byte) (0xfb));
	}

	public void exeute(CPU c) throws illegalAddressException, DeviceUnavailable, ROException {
		// TODO Auto-generated method stub
		int interuptHandlerAddress = 0; 
		if (c.bus.IsNMInterruptRaised()) {
			interuptHandlerAddress = getNMInterruptHandlerAddress(c);
		} else {
			if (c.bus.IsInterruptRaised() ) {
				c.IFLAG.set();
				interuptHandlerAddress = getInterruptHandlerAddress(c);
				c.bus.clearInterupt();
			} else {
				if (c.bus.IsPowerOnResetRased()) {
					interuptHandlerAddress = getResetHandlerAddress(c);
				}
			}
		}
		if (interuptHandlerAddress != 0) {

			int upper = (((c.pc) & 0xff00) >> 8);
			int lower = (((c.pc) & 0x00ff));

			// push on stack
			c.sp--;
			c.bus.write(c.sp, (byte) (upper & 0xff));
			c.sp--;
			c.bus.write(c.sp, (byte) (lower & 0xff));
			c.sp--;
			c.bus.write(c.sp, psr(c));

			c.pc = interuptHandlerAddress;
		}
	}

}
