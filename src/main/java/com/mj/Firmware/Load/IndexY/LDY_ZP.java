package com.mj.Firmware.Load.IndexY;

import com.mj.Firmware.Framework.Instruction;
import com.mj.cpu001.CPU;
import com.mj.exceptions.DeviceUnavailable;
import com.mj.exceptions.illegalAddressException;
import com.mj.exceptions.nflagException;
import com.mj.exceptions.zflagException;

public class LDY_ZP extends Instruction {
/*
 *  Loads X register from zero page
 */
	public LDY_ZP() {
		super((byte) 0xa4);
		setProperty(KEY_MNEMONIC, "LDY");
		setProperty(KEY_ADDRESSING_MODE, VALUE_ADDM_ZP);
		setProperty(KEY_OPCODE, "0xa4");
		setProperty(KEY_INSTRUCTION_SIZE, "2");
		setProperty(KEY_CYCLES, "3");
		setProperty(KEY_FLAGS_EFFECTED, "Z,N");
		setProperty(KEY_WEB,"http://www.obelisk.me.uk/index.html" );
		setProperty(KEY_DESCRIPTION, "Loads a byte of memory from zero page into the X register setting the zero and negative flags as appropriate.");

	}
	public void exeute(CPU c) throws illegalAddressException, DeviceUnavailable {
		// TODO Auto-generated method stub
		int zeropageAddress  = getZeroPageAddress(c); // read the zero page address - operand 
		byte value = c.bus.read(zeropageAddress);
		try {
			c.ZFLAG.clear();
			c.NFLAG.clear();
			c.y.set(value);
		} catch (zflagException e) {
			handleZException(c);
		} catch (nflagException e) {
			handleNException(c);
		}
	
		c.pc++;
	}
}
