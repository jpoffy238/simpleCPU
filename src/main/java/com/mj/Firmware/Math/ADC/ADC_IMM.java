package com.mj.Firmware.Math.ADC;

import com.mj.Firmware.Framework.Instruction;
import com.mj.Firmware.Math.Math_Abstract;
import com.mj.cpu001.CPU;
import com.mj.exceptions.DeviceUnavailable;
import com.mj.exceptions.illegalAddressException;
import com.mj.exceptions.nflagException;
import com.mj.exceptions.zflagException;

public class ADC_IMM extends Math_Abstract {
	public ADC_IMM() {
		super((byte)0x69);
		// TODO Auto-generated constructor stub
		setProperty(KEY_MNEMONIC, "ADC");
		setProperty(KEY_ADDRESSING_MODE, VALUE_ADDM_IMM);
		setProperty(KEY_OPCODE, "0x69");
		setProperty(KEY_INSTRUCTION_SIZE, "2");
		setProperty(KEY_CYCLES, "2");
		setProperty(KEY_WEB,"http://www.obelisk.me.uk/6502/reference.html#ADC" );
		setProperty(KEY_DESCRIPTION, "DC - Add with Carry\r A,Z,C,N = A+M+C");
	}

	public void exeute(CPU c) throws illegalAddressException, DeviceUnavailable {

		int m = (c.bus.read(c.pc) & 0x00ff);
		
		if (c.DFLAG.isSet()) {
			AddBCD(c, m);
		} else {
			AddCarryBinaryMode(c, m);
		}
		c.pc++;
	}
}
