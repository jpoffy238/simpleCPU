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
				c.IFLAG.set();
				c.bus.clearInterupt();
				if (interuptHandlerAddress != 0xf000) {
					logger.debug("BAD-INTERRUPT ADDRESS -- INT/BRK === INTERRUPT=============================================================");
				}
			} else {
				if (c.bus.IsPowerOnResetRased()) {
					logger.debug("RESET === INTERRUPT=============================================================");
					interuptHandlerAddress = getResetHandlerAddress(c);
					c.bus.clearpowerOnReset();
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
			logger.debug(String.format("Interrupt Address = %04x " , interuptHandlerAddress));
			c.pc = interuptHandlerAddress;
			
			
		}
	}

}
