package com.mj.Firmware.incdec;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.mj.Firmware.Framework.Instruction;
import com.mj.cpu001.CPU;
import com.mj.exceptions.DeviceUnavailable;
import com.mj.exceptions.ROException;
import com.mj.exceptions.illegalAddressException;

public class DEC_ABS extends Instruction {
	 private static final Logger logger = LogManager.getLogger(DEC_ABS.class);

	public DEC_ABS() {
		super((byte)0xce);
	}
	public void exeute(CPU c) throws illegalAddressException, DeviceUnavailable, ROException {
		int address = getAbsoluteAddress(c);
		
		int data = c.bus.read(address);
		
		String sadd = String.format("ABS %04x value %02x ", address, (byte)(data&0xff));
		logger.debug("READ:" + sadd);
		data--;
		data &= 0x00ff;
		c.ZFLAG.clear();
		c.NFLAG.clear();
		if (data == 0) {
			handleZException(c);
		}
		if ((data & 0x0080) != 0) {
			handleNException(c);
		}
		sadd = String.format("ABS %04x value %02x ", address, (byte)(data&0xff));
		logger.debug("WRITE:" + sadd);
		c.bus.write(address, (byte)data);
		c.pc+=2;
		
	}
}
