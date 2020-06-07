package com.mj.Firmware.store.Accumulator;

import com.mj.Firmware.Framework.Instruction;
import com.mj.cpu001.CPU;
import com.mj.exceptions.DeviceUnavailable;
import com.mj.exceptions.illegalAddressException;

public class STA_ZPX extends Instruction {
	
	public STA_ZPX() {
		super((byte)0x95);
		setProperty(KEY_MNEMONIC, "STA");
		setProperty(KEY_ADDRESSING_MODE, VALUE_ADDM_ZPX);
		setProperty(KEY_OPCODE, "0x95");
		setProperty(KEY_INSTRUCTION_SIZE, "2");
		setProperty(KEY_CYCLES, "4");
		setProperty(KEY_WEB,"http://6502.org/tutorials/6502opcodes.html#STA" );
		setProperty(KEY_DESCRIPTION ,"M = A  Stores the contents of the accumulator into zero page+x");
	}
	public void exeute(CPU c) throws illegalAddressException, DeviceUnavailable {
		// TODO Auto-generated method stub
		/*
		 * http://www.obelisk.me.uk/6502/addressing.html#ZPX
		 * 
		 * Zero Page,X
		 * 
		 * The address to be accessed by an instruction using indexed zero page 
		 * addressing is calculated by taking the 8 bit zero page address from 
		 * the instruction and adding the current value of the X register to it. 
		 * For example if the X register contains $0F and the instruction 
		 * LDA $80,X is executed then the accumulator will be loaded from 
		 * $008F (e.g. $80 + $0F => $8F).
		 */

		
		int  zeroPageAddress = getZeroPageXAddress(c);
		c.memory.write(zeroPageAddress, (byte)(c.a.get() & 0xff));
		c.pc++;
	}
}
