package com.mj.Firmware.ExecutionFlow;

import com.mj.Firmware.Framework.Instruction;
import com.mj.cpu001.CPU;
import com.mj.exceptions.DeviceUnavailable;
import com.mj.exceptions.illegalAddressException;

public class JMP_IND extends Instruction {
private String VALUE_ADDM_IND;
	/*
 * JMP (JuMP)
Affects Flags: none

MODE           SYNTAX       HEX LEN TIM
Absolute      JMP $5597     $4C  3   3
Indirect      JMP ($5597)   $6C  3   5

JMP transfers program execution to the 
following address (absolute) or to the location 
contained in the following address (indirect). 
Note that there is no carry associated with the 
indirect jump so:
AN INDIRECT JUMP MUST NEVER USE A
VECTOR BEGINNING ON THE LAST BYTE
OF A PAGE
For example if address $3000 contains $40, $30FF 
contains $80, and $3100 contains $50, the result of 
JMP ($30FF) will be a transfer of control to $4080 
rather than $5080 as you intended i.e. the 6502
 took the low byte of the address from $30FF and 
 the high byte from $3000.
 
 */
	public JMP_IND () {
		super((byte)0x6c);
		setProperty(KEY_MNEMONIC, "JMP");
		setProperty(KEY_ADDRESSING_MODE, VALUE_ADDM_IND);
		setProperty(KEY_OPCODE, "0x6c");
		setProperty(KEY_INSTRUCTION_SIZE, "3");
		setProperty(KEY_CYCLES, "5");
		setProperty(KEY_WEB,"http://6502.org/tutorials/6502opcodes.html#JMP" );
		setProperty(KEY_DESCRIPTION, "MP transfers program execution to the following address (absolute) . ");
	}
	public void exeute(CPU c) throws illegalAddressException, DeviceUnavailable {
		// TODO Auto-generated method stub
		int address = getIndirect(c);
		
		c.pc =address;
	}

	

}
