package com.mj.Firmware.incdec;

import com.mj.Firmware.Framework.Instruction;
import com.mj.cpu001.CPU;
import com.mj.exceptions.DeviceUnavailable;
import com.mj.exceptions.illegalAddressException;
import com.mj.exceptions.nflagException;
import com.mj.exceptions.zflagException;

public class DEX extends Instruction {

	public DEX() {
		super((byte)0xca);
		setProperty(KEY_MNEMONIC, "DEX");
		setProperty(KEY_ADDRESSING_MODE, "IMPLIED");
		setProperty(KEY_OPCODE, "0xca");
		setProperty(KEY_INSTRUCTION_SIZE, "1");
		setProperty(KEY_CYCLES, "2");
		setProperty(KEY_FLAGS_EFFECTED, "Z,N");
		setProperty(KEY_WEB,"http://www.obelisk.me.uk/6502/reference.html#DEX" );
		setProperty(KEY_DESCRIPTION, "Decrement X register by 1 - set flags as needed ");
	}
	public void exeute(CPU c) throws illegalAddressException, DeviceUnavailable {
		try {
			c.ZFLAG.clear();
			c.NFLAG.clear();
			c.x.dec();
		} catch (nflagException e) {
			handleNException(c);
		} catch (zflagException e) {
			// TODO Auto-generated catch block
			handleZException(c);
		}
	
		
	}
}
