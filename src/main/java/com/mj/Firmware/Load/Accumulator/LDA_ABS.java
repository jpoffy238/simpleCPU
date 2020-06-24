package com.mj.Firmware.Load.Accumulator;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.mj.Firmware.Framework.Instruction;
import com.mj.cpu001.CPU;
import com.mj.exceptions.DeviceUnavailable;
import com.mj.exceptions.illegalAddressException;
import com.mj.exceptions.nflagException;
import com.mj.exceptions.zflagException;

public class LDA_ABS extends Instruction {
	// Zero Page reference (X)
	 private static final Logger logger = LogManager.getLogger(LDA_ABS.class);
	public LDA_ABS() {
		super ((byte)0xad);
		setProperty(KEY_MNEMONIC, "LDA");
		setProperty(KEY_ADDRESSING_MODE, VALUE_ADDM_ABS);
		setProperty(KEY_OPCODE, "0xad");
		setProperty(KEY_INSTRUCTION_SIZE, "3");
		setProperty(KEY_CYCLES, "3");
		setProperty(KEY_FLAGS_EFFECTED, "Z,N");
		setProperty(KEY_WEB,"http://www.obelisk.me.uk/6502/reference.html#LDA" );
		setProperty(KEY_DESCRIPTION, "A,Z,N = M - Loads a byte of memory from "
				+ "absolute memory operand  into the accumulator setting the zero and negative flags as appropriate.");
	}
	public void exeute(CPU c) throws illegalAddressException, DeviceUnavailable {
		// TODO Auto-generated method stub
		int address = getAbsoluteAddress(c);
		byte m = c.bus.read(address);
		String currentState = String.format("%-10s $(%-4x) Value[ %-2x]", getProperty(KEY_MNEMONIC), address, (int)(m & 0xff));
		logger.debug(currentState);
		try {
			c.a.set(m);
		} catch (zflagException e) {
			handleZException(c);
		} catch (nflagException e) {
			handleNException(c);
		}
		c.pc+=2;
	}
}
