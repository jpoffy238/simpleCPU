package com.mj.Firmware.Load.IndexX;

import com.mj.Firmware.Framework.Instruction;
import com.mj.cpu001.CPU;
import com.mj.exceptions.DeviceUnavailable;
import com.mj.exceptions.illegalAddressException;
import com.mj.exceptions.nflagException;
import com.mj.exceptions.zflagException;

public class LDX_IMM extends Instruction {
	//TODO Setup correct opcode - using default
public LDX_IMM() {
	super((byte)0xa2);
	setProperty(KEY_MNEMONIC, "LDX");
	setProperty(KEY_ADDRESSING_MODE, VALUE_ADDM_IMM);
	setProperty(KEY_OPCODE, "0xa2");
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
			c.ZFLAG.clear();
			c.NFLAG.clear();
			c.x.set(m);
		} catch (zflagException e) {
			c.ZFLAG.set();
			c.NFLAG.clear();
		} catch (nflagException e) {
			c.NFLAG.set();
			c.ZFLAG.clear();
		}
	
		c.pc++;
	}
}
