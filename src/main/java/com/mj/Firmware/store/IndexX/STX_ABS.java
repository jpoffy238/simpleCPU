package com.mj.Firmware.store.IndexX;

import com.mj.Firmware.Framework.Instruction;
import com.mj.cpu001.CPU;
import com.mj.exceptions.DeviceUnavailable;
import com.mj.exceptions.ROException;
import com.mj.exceptions.illegalAddressException;

public class STX_ABS extends Instruction {
public STX_ABS() {
	super((byte)0x86);
	setProperty(KEY_MNEMONIC, "STX");
	setProperty(KEY_ADDRESSING_MODE, VALUE_ADDM_ABS);
	setProperty(KEY_OPCODE, "0x86");
	setProperty(KEY_INSTRUCTION_SIZE, "2");
	setProperty(KEY_CYCLES, "3");
	setProperty(KEY_WEB,"http://6502.org/tutorials/6502opcodes.html#STA" );
	setProperty(KEY_DESCRIPTION ,"M = X  Stores the contents of the accumulator into memory.");
}
	public void exeute(CPU c) throws illegalAddressException, DeviceUnavailable, ROException {
		
		int address = getAbsoluteAddress(c);
		c.bus.write(address, (byte)(c.x.get() & 0xff));
		c.pc+=2;
	}
}
