package Firmeware.Logic;

import Firmeware.Framework.Instruction;
import cpu001.CPU;
import exceptions.DeviceUnavailable;
import exceptions.illegalAddressException;

public class CMP extends Instruction {
/*
 * CMP (CoMPare accumulator)
Affects Flags: N Z C

MODE           SYNTAX       HEX LEN TIM
Immediate     CMP #$44      $C9  2   2
Zero Page     CMP $44       $C5  2   3
Zero Page,X   CMP $44,X     $D5  2   4
Absolute      CMP $4400     $CD  3   4
Absolute,X    CMP $4400,X   $DD  3   4+
Absolute,Y    CMP $4400,Y   $D9  3   4+
Indirect,X    CMP ($44,X)   $C1  2   6
Indirect,Y    CMP ($44),Y   $D1  2   5+

+ add 1 cycle if page boundary crossed

Compare sets flags as if a subtraction had been carried out. 
If the value in the accumulator is equal or greater than the 
compared value, the Carry will be set. The equal (Z) and 
negative (N) flags will be set based on equality or lack there
of and the sign (i.e. A>=$80) of the accumulator.
 
 */
	public CMP () {
		super((byte)0x44);
		setProperty(KEY_MNEMONIC, "CMP");
		setProperty(KEY_ADDRESSING_MODE, VALUE_ADDM_IMM);
		setProperty(KEY_OPCODE, "0x44");
		setProperty(KEY_INSTRUCTION_SIZE, "2");
		setProperty(KEY_CYCLES, "2");
		setProperty(KEY_WEB,"http://6502.org/tutorials/6502opcodes.html#CMP" );
		setProperty(KEY_DESCRIPTION, "Compare sets flags as if a subtraction had been carried out. ");
	}
	
	
	public void exeute(CPU c) throws illegalAddressException, DeviceUnavailable {
		// TODO Auto-generated method stub
		int m = c.memory.read(c.pc);
		c.pc = (++c.pc);
		int a = c.a.get();
		
		int result = a - m;
		if (result == 0) {
			c.ZFLAG.set();
			c.NFLAG.clear();
		} else {
			if (result < 0) {
				c.NFLAG.set();
				c.ZFLAG.clear();
			}  else {
				c.NFLAG.clear();
				c.ZFLAG.clear();
			}
		}
	}

	

}