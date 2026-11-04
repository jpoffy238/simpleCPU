package com.mj.Firmware.Framework;

import com.mj.cpu001.CPU;
import com.mj.exceptions.DeviceUnavailable;
import com.mj.exceptions.ROException;
import com.mj.exceptions.illegalAddressException;

public class MIH extends Instruction {

	public MIH() {
		super((byte) (0xfb));
	}

	/**
	 * Machine interrupt handler. Priority is RESET, then NMI, then IRQ (which is
	 * ignored while the I flag is set). RESET loads the PC from the reset vector
	 * without touching the stack; NMI and IRQ push PCH, PCL and the status (B
	 * clear), set I, and load the PC from their vector.
	 */
	public void exeute(CPU c) throws illegalAddressException, DeviceUnavailable, ROException {
		int vector;
		if (c.bus.IsPowerOnResetRased()) {
			logger.debug("RESET interrupt");
			c.bus.clearpowerOnReset();
			c.reset();                      // loads PC from the reset vector
			return;
		} else if (c.bus.IsNMInterruptRaised()) {
			logger.debug("NMI interrupt");
			vector = getNMInterruptHandlerAddress(c);
			c.bus.clearNMInterupt();
		} else if (c.bus.IsInterruptRaised() && !c.IFLAG.isSet()) {
			logger.debug("IRQ interrupt");
			vector = getInterruptHandlerAddress(c);
			c.bus.clearInterupt();
		} else {
			return;                         // nothing to service (e.g. IRQ masked)
		}

		c.push((byte) ((c.pc >> 8) & 0xff));
		c.push((byte) (c.pc & 0xff));
		c.push(psr(c, false));              // hardware interrupt: B bit clear
		c.IFLAG.set();                      // I is set after the old status is saved
		logger.debug(String.format("Interrupt Address = %04x ", vector));
		c.pc = vector;
	}

}
