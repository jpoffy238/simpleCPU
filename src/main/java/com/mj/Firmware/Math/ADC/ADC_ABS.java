package com.mj.Firmware.Math.ADC;

import com.mj.Firmware.Framework.Instruction;
import com.mj.cpu001.CPU;
import com.mj.exceptions.DeviceUnavailable;
import com.mj.exceptions.illegalAddressException;
import com.mj.exceptions.nflagException;
import com.mj.exceptions.zflagException;
import com.mj.Firmware.Math.ADC.ADC_Abstract;

public class ADC_ABS extends ADC_Abstract {
	public ADC_ABS() {
		super((byte) 0x6d);
		// TODO Auto-generated constructor stub
		setProperty(KEY_MNEMONIC, "ADC");
		setProperty(KEY_ADDRESSING_MODE, VALUE_ADDM_ABS);
		setProperty(KEY_OPCODE, "0x6d");
		setProperty(KEY_INSTRUCTION_SIZE, "3");
		setProperty(KEY_CYCLES, "4");
		setProperty(KEY_WEB, "http://www.obelisk.me.uk/6502/reference.html#ADC");
		setProperty(KEY_DESCRIPTION, "DC - Add with Carry\r A,Z,C,N = A+M+C");
	}

	public void exeute(CPU c) throws illegalAddressException, DeviceUnavailable {

		int address = getAbsoluteAddress(c);
		int m = (byte) (c.bus.read(address) & 0xff);

		if (c.DFLAG.isSet()) {
			AddBCD(c, m);
		} else {
			AddCarryBinaryMode(c, m);
		}
		c.pc += 2;
	}
}
