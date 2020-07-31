package com.mj.Firmware.Load.IndexY;

import com.mj.Firmware.Framework.Instruction;
import com.mj.cpu001.CPU;
import com.mj.exceptions.DeviceUnavailable;
import com.mj.exceptions.illegalAddressException;
import com.mj.exceptions.nflagException;
import com.mj.exceptions.zflagException;

public class LDY_IMM extends Instruction {
	//TODO Setup correct opcode - using default
public LDY_IMM() {
	super((byte)0xa0);
	setProperty(KEY_MNEMONIC, "LDY");
	setProperty(KEY_ADDRESSING_MODE, VALUE_ADDM_IMM);
	setProperty(KEY_OPCODE, "0xa0");
	setProperty(KEY_INSTRUCTION_SIZE, "2");
	setProperty(KEY_CYCLES, "2");
	setProperty(KEY_FLAGS_EFFECTED, "Z,N");
	setProperty(KEY_WEB,"http://www.obelisk.me.uk/index.html" );
	setProperty(KEY_DESCRIPTION, "Loads a byte of memory into the X register setting the zero and negative flags as appropriate.");
}
	public void exeute(CPU c) throws illegalAddressException, DeviceUnavailable {
		// TODO Auto-generated method stub
		byte m = c.bus.read(c.pc);
		try {
			c.y.set(m);
		} catch (zflagException e) {
			handleZException(c);
		} catch (nflagException e) {
			handleNException(c);
		}
	
		c.pc++;
	}
}
