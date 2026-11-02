package com.mj.Firmware.Logic.Shift.LSR;

import com.mj.Firmware.Framework.Instruction;
import com.mj.cpu001.CPU;
import com.mj.exceptions.DeviceUnavailable;
import com.mj.exceptions.ROException;
import com.mj.exceptions.illegalAddressException;
import com.mj.exceptions.nflagException;
import com.mj.exceptions.zflagException;

/** LSR - Logical Shift Right (Absolute). C = old bit 0, N is always cleared. */
public class LSR_ABS extends Instruction {
	public LSR_ABS() {
		super((byte) (0x4e));
		setProperty(KEY_MNEMONIC, "LSR");
		setProperty(KEY_ADDRESSING_MODE, VALUE_ADDM_ABS);
		setProperty(KEY_OPCODE, "0x4e");
		setProperty(KEY_INSTRUCTION_SIZE, "3");
		setProperty(KEY_CYCLES, "6");
		setProperty(KEY_FLAGS_EFFECTED, "N Z C");
		setProperty(KEY_WEB, "http://6502.org/tutorials/6502opcodes.html#LSR");
		setProperty(KEY_DESCRIPTION, "LSR - Logical Shift Right. Shift A or memory right by 1; bit 0 goes to the carry flag.");
	}

	public void exeute(CPU c) throws illegalAddressException, DeviceUnavailable, ROException {
		int address = getAbsoluteAddress(c);
		int result = lsrAndSetFlags(c, c.bus.read(address));
		c.bus.write(address, (byte) result);
		c.pc += 2;
	}

}
