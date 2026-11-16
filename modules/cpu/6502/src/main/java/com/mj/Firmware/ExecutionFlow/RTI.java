package com.mj.Firmware.ExecutionFlow;

import com.mj.Firmware.Framework.Instruction;
import com.mj.cpu001.CPU;
import com.mj.exceptions.DeviceUnavailable;
import com.mj.exceptions.illegalAddressException;

public class RTI extends Instruction {
	/*
	 * RTI (ReTurn from Interrupt)
Affects Flags: all (restored from the stack)

MODE           SYNTAX       HEX LEN TIM
Implied       RTI           $40  1   6

RTI retrieves the processor status word, then the program counter,
from the stack in that order - the reverse of how BRK/IRQ/NMI pushed
them. Unlike RTS, the pulled PC is used as-is (no +1), since the
pushed address is already the real resume address.
	 */
	public RTI( ) {
		super((byte)0x40);
		setProperty(KEY_MNEMONIC, "RTI");
		setProperty(KEY_ADDRESSING_MODE, VALUE_ADDM_STK);
		setProperty(KEY_OPCODE, "0x40");
		setProperty(KEY_INSTRUCTION_SIZE, "1");
		setProperty(KEY_CYCLES, "6");
		setProperty(KEY_WEB,"http://6502.org/tutorials/6502opcodes.html#RTI" );
		setProperty(KEY_DESCRIPTION ,"RTI (ReTurn from Interrupt) pulls the status register, then the " +
				"two-byte program counter, off the stack and resumes execution there - " +
				"the counterpart to BRK/IRQ/NMI, which push them in reverse order.");

	}
	
	public void exeute(CPU c) throws illegalAddressException, DeviceUnavailable {
		// return from  Subroutine  
		// pop the lower  order PC from  stack 
		// pop  high  order PC from  stack
		// 
		
			byte cpstatus = c.pull();
			setFlags(c, cpstatus);
			int lower = c.pull() & 0xff;
			int upper = c.pull() & 0xff;
			c.pc = (upper << 8) | lower;
		}
}
