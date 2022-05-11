package com.mj.Firmware.Logic;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.mj.Firmware.Framework.Instruction;
import com.mj.cpu001.CPU;
import com.mj.exceptions.DeviceUnavailable;
import com.mj.exceptions.illegalAddressException;

public class BIT_ABS extends Instruction {
	/*
	 * Affect Flags: none
	 * 
	 BIT - Bit Test
		A & M, N = M7, V = M6

This instructions is used to test if one or more bits are set in a target 
memory location. The mask pattern in A is ANDed with the value in memory 
to set or clear the zero flag, but the result is not kept. Bits 7 and 6 of the 
value from memory are copied into the N and V flags.

Processor Status after use:
	 */
	  private static final Logger logger = LogManager.getLogger(BIT_ABS.class);
	public BIT_ABS() {
		super((byte) (0x2c));
		setProperty(KEY_MNEMONIC, "BIT");
		setProperty(KEY_ADDRESSING_MODE, VALUE_ADDM_ABS);
		setProperty(KEY_OPCODE, "0x2c");
		setProperty(KEY_INSTRUCTION_SIZE, "3");
		setProperty(KEY_CYCLES, "4");
		setProperty(KEY_FLAGS_EFFECTED, "Z,N,O");
		setProperty(KEY_WEB, "http://6502.org/tutorials/6502opcodes.html#BIT");
		setProperty(KEY_DESCRIPTION, "BCC  (Branch on Carry  set )  If Carry  flag is set");

	}

	public void exeute(CPU c) throws illegalAddressException, DeviceUnavailable {
		// TODO Auto-generated method stub
		int testAddress = getAbsoluteAddress(c);
		byte testValue = c.bus.read(testAddress);
		String currentState = String.format("%-10s $(%-4x) Value[ %-2x]", getProperty(KEY_MNEMONIC), testAddress, (int)(testValue));
		logger.debug(currentState);
		c.ZFLAG.clear();
		c.NFLAG.clear();
		c.OFLAG.clear();
		if ((0x80 & testValue) != 0) {
			c.NFLAG.set();
		}
		if ((0x40 & testValue ) != 0 ) {
			c.OFLAG.set();
		}
		int result = testValue & (byte) (c.a.get() & 0xff);
		if (result == 0) {
			c.ZFLAG.set();
		}
		c.pc += 2;
		logger.debug(c.dump());
	}

}
