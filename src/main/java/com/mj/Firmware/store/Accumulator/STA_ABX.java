package com.mj.Firmware.store.Accumulator;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.mj.Firmware.Framework.Instruction;
import com.mj.cpu001.CPU;
import com.mj.exceptions.DeviceUnavailable;
import com.mj.exceptions.ROException;
import com.mj.exceptions.illegalAddressException;

public class STA_ABX extends Instruction {
	static Logger logger = LogManager.getLogger(STA_ABX.class);
public STA_ABX() {
	super((byte)0x9d);
	setProperty(KEY_MNEMONIC, "STA");
	setProperty(KEY_ADDRESSING_MODE, VALUE_ADDM_ABX);
	setProperty(KEY_OPCODE, "0x9d");
	setProperty(KEY_INSTRUCTION_SIZE, "3");
	setProperty(KEY_CYCLES, "4");
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
		int address = getAbsoluteAddressX(c);
		logger.debug(String.format("W: Address %04x Value %02x", address, c.a.get()));
		c.bus.write(address, (byte)(c.a.get() & 0xff));
		c.pc +=2;
	}
}
