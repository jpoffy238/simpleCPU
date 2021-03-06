package com.mj.Firmware.store.Accumulator;

import com.mj.Firmware.Framework.Instruction;
import com.mj.cpu001.CPU;
import com.mj.exceptions.DeviceUnavailable;
import com.mj.exceptions.ROException;
import com.mj.exceptions.illegalAddressException;

public class STA_ZP extends Instruction {
public STA_ZP() {
	super((byte)0x85);
	setProperty(KEY_MNEMONIC, "STA");
	setProperty(KEY_ADDRESSING_MODE, VALUE_ADDM_ZP);
	setProperty(KEY_OPCODE, "0x85");
	setProperty(KEY_INSTRUCTION_SIZE, "2");
	setProperty(KEY_CYCLES, "3");
	setProperty(KEY_WEB,"http://6502.org/tutorials/6502opcodes.html#STA" );
	setProperty(KEY_DESCRIPTION ,"M = A  Stores the contents of the accumulator into memory.");
}
	public void exeute(CPU c) throws illegalAddressException, DeviceUnavailable, ROException {
		// TODO Auto-generated method stub
		/* 
		 * Zero Page Address
		 * An instruction using zero page addressing mode has 
		 * only an 8 bit address operand. This limits it to addressing
		 * only the first 256 bytes of memory (e.g. $0000 to $00FF) 
		 * where the most significant byte of the address is always 
		 * zero. In zero page mode only the least significant byte of 
		 * the address is held in the instruction making it shorter by 
		 * one byte (important for space saving) and one less 
		 * memory fetch during execution (important for speed).
		 * An assembler will automatically select zero page addressing mode
		 *  if the operand evaluates to a zero page address and the 
		 *  instruction supports the mode (not all do).
		 */
		int zeroPageAddress = getZeroPageAddress(c);
		
		c.bus.write(zeroPageAddress, (byte)(c.a.get() & 0xff));
		c.pc++;
	}
}
