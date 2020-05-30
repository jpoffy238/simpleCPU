package Firmeware.store.IndexX;

import Firmeware.Framework.Instruction;
import cpu001.CPU;
import exceptions.DeviceUnavailable;
import exceptions.illegalAddressException;

public class STX_ZPY extends Instruction {
public STX_ZPY() {
	super((byte)0x96);
	setProperty(KEY_MNEMONIC, "STA");
	setProperty(KEY_ADDRESSING_MODE, VALUE_ADDM_ABS);
	setProperty(KEY_OPCODE, "0x96");
	setProperty(KEY_INSTRUCTION_SIZE, "2");
	setProperty(KEY_CYCLES, "4");
	setProperty(KEY_WEB,"http://6502.org/tutorials/6502opcodes.html#STA" );
	setProperty(KEY_DESCRIPTION ,"M = A  Stores the contents of the accumulator into memory.");
}
	public void exeute(CPU c) throws illegalAddressException, DeviceUnavailable {
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
		int address = getZeroPageYAddress(c);
		
		c.memory.write(address, (byte)(c.x.get() & 0xff));
		c.pc++;
	}
}
