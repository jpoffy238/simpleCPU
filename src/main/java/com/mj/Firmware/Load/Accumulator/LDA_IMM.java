package com.mj.Firmware.Load.Accumulator;

import com.mj.Firmware.Framework.Instruction;
import com.mj.cpu001.CPU;
import com.mj.exceptions.DeviceUnavailable;
import com.mj.exceptions.illegalAddressException;
import com.mj.exceptions.nflagException;
import com.mj.exceptions.zflagException;

public class LDA_IMM extends Instruction {
// Load from Immediate on byte following
// instructions.
	public LDA_IMM () {
		super((byte)0xa9);
		setProperty(KEY_MNEMONIC, "LDA");
		setProperty(KEY_ADDRESSING_MODE, VALUE_ADDM_IMM);
		setProperty(KEY_OPCODE, "0xa9");
		setProperty(KEY_INSTRUCTION_SIZE, "2");
		setProperty(KEY_CYCLES, "2");
		setProperty(KEY_FLAGS_EFFECTED, "Z,N");
		setProperty(KEY_WEB,"http://www.obelisk.me.uk/6502/reference.html#LDA" );
		setProperty(KEY_DESCRIPTION, "A,Z,N = M - Loads a byte of memory into the accumulator setting the zero and negative flags as appropriate.");
	}
	public void exeute(CPU c) throws illegalAddressException, DeviceUnavailable {
		// TODO Auto-generated method stub
		/*
		 *  Immediate
		 * 
		 * Immediate addressing allows the programmer to directly 
		 * specify an 8 bit constant within the instruction. It is indicated 
		 * by a '#' symbol followed by an numeric expression. 
		 * For example:
		 *         LDA #10         ;Load 10 ($0A) into the accumulator
		 *         LDX #LO LABEL   ;Load the LSB of a 16 bit address into X
		 *         LDY #HI LABEL   ;Load the MSB of a 16 bit address into Y
		 */
		byte m = c.bus.read(c.pc);
		String currentState = String.format("%-10s  #$ %-2x", getProperty(KEY_MNEMONIC),  (int)(m & 0xff));
		logger.debug(currentState);
		c.pc = (++c.pc);
		try {
			c.ZFLAG.clear();
			c.NFLAG.clear();
			c.a.set(m);
		} catch (zflagException z) {
			handleZException(c);
		} catch (nflagException n) {
			handleNException(c);
		}
		
	}

	


}
