package com.mj.Firmware.Framework;

import com.mj.cpu001.CPU;
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
		logger.debug("INTERRUPT=============================================================");
		if (c.bus.IsNMInterruptRaised()) {
			logger.debug("NM === INTERRUPT=============================================================");
			interuptHandlerAddress = getNMInterruptHandlerAddress(c);
			c.bus.clearNMInterupt();
		} else {
			if (c.bus.IsInterruptRaised() ) {
				logger.debug("INT/BRK === INTERRUPT=============================================================");
				
				interuptHandlerAddress = getInterruptHandlerAddress(c);
				c.bus.clearInterupt();
			} else {
				if (c.bus.IsPowerOnResetRased()) {
					logger.debug("RESET === INTERRUPT=============================================================");
					interuptHandlerAddress = getResetHandlerAddress(c);
					c.bus.clearpowerOnReset();
					c.reset();
					c.pc = interuptHandlerAddress;
					// this is a special case -  need to not push cpu status on stack.
					return;
				}
			}
		}
		if (interuptHandlerAddress != 0) {

			int upper = (((c.pc) & 0xff00) >> 8);
			int lower = (((c.pc) & 0x00ff));

			// push on stack: PCH, PCL, then status (B clear for hardware interrupts)
			c.push((byte) (upper & 0xff));
			c.push((byte) (lower & 0xff));
			c.push(psr(c, false));
			c.IFLAG.set();                       // I is set after the old status is saved
			logger.debug(String.format("Interrupt Address = %04x " , interuptHandlerAddress));
			c.pc = interuptHandlerAddress;
			
			
		}
	}

}
