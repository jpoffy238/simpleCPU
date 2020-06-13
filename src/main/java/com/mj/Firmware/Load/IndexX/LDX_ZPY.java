package com.mj.Firmware.Load.IndexX;

import com.mj.Firmware.Framework.Instruction;
import com.mj.cpu001.CPU;
import com.mj.exceptions.DeviceUnavailable;
import com.mj.exceptions.illegalAddressException;
import com.mj.exceptions.nflagException;
import com.mj.exceptions.zflagException;

public class LDX_ZPY extends Instruction {
/*
 *  Loads X register from zero page
 */
	public LDX_ZPY() {
		super((byte) 0xb6);
		setProperty(KEY_MNEMONIC, "LDX");
		setProperty(KEY_ADDRESSING_MODE, VALUE_ADDM_ZPY);
		setProperty(KEY_OPCODE, "0xb6");
		setProperty(KEY_INSTRUCTION_SIZE, "2");
		setProperty(KEY_CYCLES, "4");
		setProperty(KEY_FLAGS_EFFECTED, "Z,N");
		setProperty(KEY_WEB,"http://www.obelisk.me.uk/index.html" );
		setProperty(KEY_DESCRIPTION, "Loads a byte of memory from zero page (opperand+Y) "
				+ " into the X register setting the zero and negative flags as appropriate.");

	}
	public void exeute(CPU c) throws illegalAddressException, DeviceUnavailable {
		// TODO Auto-generated method stub
		int loadAdress = getZeroPageYAddress(c);
		
		byte value = c.bus.read(loadAdress);
		try {
			c.x.set(value);
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
