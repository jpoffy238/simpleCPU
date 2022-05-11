package com.mj.Firmware.Math.ADC;

import com.mj.Firmware.Math.Math_Abstract;
import com.mj.cpu001.CPU;
import com.mj.exceptions.DeviceUnavailable;
import com.mj.exceptions.illegalAddressException;

public class ADC_ZPX extends Math_Abstract {
	public ADC_ZPX() {
		super((byte)0x75);
		// TODO Auto-generated constructor stub
		setProperty(KEY_MNEMONIC, "ADC");
		setProperty(KEY_ADDRESSING_MODE, VALUE_ADDM_ZPX);
		setProperty(KEY_OPCODE, "0x75");
		setProperty(KEY_INSTRUCTION_SIZE, "2");
		setProperty(KEY_CYCLES, "4");
		setProperty(KEY_WEB,"http://www.obelisk.me.uk/6502/reference.html#ADC" );
		setProperty(KEY_DESCRIPTION, "DC - Add with Carry\r A,Z,C,N = A+M+C");
	}

	public void exeute(CPU c) throws illegalAddressException, DeviceUnavailable {

		int address = getZeroPageXAddress(c);
		int m = (byte)(c.bus.read(address) & 0xff);
		
		if (c.DFLAG.isSet()) {
			AddBCD2(c, m);
		} else {
			AddCarryBinaryMode(c, m);
		}
	
		c.pc++;
	}
}
