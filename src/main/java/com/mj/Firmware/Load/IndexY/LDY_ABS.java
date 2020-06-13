package com.mj.Firmware.Load.IndexY;

import com.mj.Firmware.Framework.Instruction;
import com.mj.cpu001.CPU;
import com.mj.exceptions.DeviceUnavailable;
import com.mj.exceptions.illegalAddressException;
import com.mj.exceptions.nflagException;
import com.mj.exceptions.zflagException;

public class LDY_ABS extends Instruction {
	public LDY_ABS() {
		super((byte) 0xac);
		setProperty(KEY_MNEMONIC, "LDY");
		setProperty(KEY_ADDRESSING_MODE, VALUE_ADDM_ABS);
		setProperty(KEY_OPCODE, "0xac");
		setProperty(KEY_INSTRUCTION_SIZE, "3");
		setProperty(KEY_CYCLES, "4");
		setProperty(KEY_FLAGS_EFFECTED, "Z,N");
		setProperty(KEY_WEB,"http://www.obelisk.me.uk/index.html" );
		setProperty(KEY_DESCRIPTION, "Loads a byte of memory from opperand"
				+ " into the X register setting the zero and negative flags as appropriate.");

	}
	public void exeute(CPU c) throws illegalAddressException, DeviceUnavailable {
		// TODO Auto-generated method stub	
		int loadAddress = getAbsoluteAddress(c);		
		byte value = c.bus.read((int)(loadAddress));
		try {
			c.y.set(value);
		} catch (zflagException e) {
			handleZException(c);
		} catch (nflagException e) {
			handleNException(c);
		}
	
		c.pc+=2;
	}
}
