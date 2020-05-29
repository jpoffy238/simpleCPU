package Firmeware.Math;

import Firmeware.Framework.Instruction;
import cpu001.CPU;

public class ADCI extends Instruction {
	public ADCI() {
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

	public void exeute(CPU c) {
		// TODO Auto-generated method stub
		// http://www.obelisk.me.uk/6502/reference.html#ADC
     	//
		// Addressing Mode 	Opcode  Bytes	Cycles
        // Immediate 	              0x69        2            2
	
		/*ADC - Add with Carry
		 *  A,Z,C,N = A+M+C
		 * This instruction adds the contents of a memory 
		 * location to the accumulator together with the 
		 * carry bit. If overflow occurs the carry bit is set, 
		 * this enables multiple byte addition to be performed.
		 * 
		 * Processor Status after use:
		 * 
		 * C 	Carry Flag 	Set if overflow in bit 7
		 * Z 	Zero Flag 	Set if A = 0
		 * I 	    Interrupt Disable 	Not affected
		 * D 	Decimal Mode Flag 	Not affected
		 * B 	Break Command 	Not affected
		 * V 	Overflow Flag 	Set if sign bit is incorrect
		 * N 	Negative Flag 	Set if bit 7 set
		 * 
		 */
		int m = c.memory.read(c.pc);
		c.pc = (++c.pc);
		int a = c.a.get();
		boolean anegative = (a & 0x80) > 0;
		boolean  mnegative  = (m & 0x80) > 0;
		
		int carryValue  = 0;
		if (c.flags.CFLAG.isSet() ) {
			carryValue = 1;
		}
		
		int result = m + c.a.get() + carryValue;
		if (result > 255 ) {
			c.flags.CFLAG.set();
		} else {
			if ((result & 0x80) != 0) {
				c.flags.OVFLAG.set();
			}
		}
		
			
		c.a.set(result & 0xff);
		if ( !anegative && !mnegative ) {
			c.flags.NFLAG.clear();
		}
		
	}
}
