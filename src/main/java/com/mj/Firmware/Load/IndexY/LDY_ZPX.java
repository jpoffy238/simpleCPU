package com.mj.Firmware.Load.IndexY;

import com.mj.Firmware.Framework.Instruction;
import com.mj.cpu001.CPU;
import com.mj.exceptions.DeviceUnavailable;
import com.mj.exceptions.illegalAddressException;
import com.mj.exceptions.nflagException;
import com.mj.exceptions.zflagException;

public class LDY_ZPX extends Instruction {
/*
 *  Loads X register from zero page
 */
	public LDY_ZPX() {
		super((byte) 0xb4);
		setProperty(KEY_MNEMONIC, "LDY");
		setProperty(KEY_ADDRESSING_MODE, VALUE_ADDM_ZPX);
		setProperty(KEY_OPCODE, "0xb4");
		setProperty(KEY_INSTRUCTION_SIZE, "2");
		setProperty(KEY_CYCLES, "4");
		setProperty(KEY_FLAGS_EFFECTED, "Z,N");
		setProperty(KEY_WEB,"http://www.obelisk.me.uk/index.html" );
		setProperty(KEY_DESCRIPTION, "Loads a byte of memory from zero page (opperand+Y) "
				+ " into the X register setting the zero and negative flags as appropriate.");

	}
	public void exeute(CPU c) throws illegalAddressException, DeviceUnavailable {
		int zeroPageAddress = getZeroPageXAddress(c); // will wrap around zero page 
		int value = c.bus.read(zeroPageAddress);
		try {
			c.y.set(value);
		} catch (zflagException e) {
			handleZException(c);
		} catch (nflagException e) {
			handleNException(c);
		}
	
		c.pc++;
	}
}
