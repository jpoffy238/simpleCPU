package Firmeware.store;

import Firmeware.Framework.Instruction;
import cpu001.CPU;

public class STAZ extends Instruction {
public STAZ() {
	super((byte)0x00);
}
	public void exeute(CPU c) {
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
		byte zeroPageAddress = c.memory.read(c.pc);
		
		c.memory.write(zeroPageAddress, (byte)(c.a.get() & 0xff));
		c.pc = (++c.pc);
	}
}
