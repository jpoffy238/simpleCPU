package com.mj.Firmware.ExecutionFlow;

import com.mj.Firmware.Framework.Instruction;
import com.mj.cpu001.CPU;
import com.mj.exceptions.DeviceUnavailable;
import com.mj.exceptions.illegalAddressException;

public class RTI extends Instruction {
	/* 
	 * RTS (ReTurn from Subroutine)
Affects Flags: none

MODE           SYNTAX       HEX LEN TIM
Implied       RTS           $60  1   6

RTS pulls the top two bytes off the stack (low byte first) and 
transfers program control to that address+1. It is used, as 
expected, to exit a subroutine invoked via JSR which pushed 
the address-1.  RTS is frequently used to implement a jump 
table where addresses-1 are pushed onto the stack and 
accessed via RTS eg. to access the second of four routines:

 LDX #1
 JSR EXEC
 JMP SOMEWHERE

LOBYTE
 .BYTE <ROUTINE0-1,<ROUTINE1-1
 .BYTE <ROUTINE2-1,<ROUTINE3-1

HIBYTE
 .BYTE >ROUTINE0-1,>ROUTINE1-1
 .BYTE >ROUTINE2-1,>ROUTINE3-1

EXEC
 LDA HIBYTE,X
 PHA
 LDA LOBYTE,X
 PHA
 RTS
	 * 
	 */
	public RTI( ) {
		super((byte)0x40);
		setProperty(KEY_MNEMONIC, "RTI");
		setProperty(KEY_ADDRESSING_MODE, VALUE_ADDM_STK);
		setProperty(KEY_OPCODE, "0x60");
		setProperty(KEY_INSTRUCTION_SIZE, "1");
		setProperty(KEY_CYCLES, "6");
		setProperty(KEY_WEB,"http://6502.org/tutorials/6502opcodes.html#RTS" );
		setProperty(KEY_DESCRIPTION ,"JRTS pulls the top two bytes off the stack (low byte first) and \n" + 
				"transfers program control to that address+1. It is used, as \n" + 
				"expected, to exit a subroutine invoked via JSR which pushed \n" + 
				"the address-1.  RTS is frequently used to implement a jump \n" + 
				"table where addresses-1 are pushed onto the stack and \n" + 
				"accessed via RTS eg. to access the second of four routines: ");
				
	}
	
	public void exeute(CPU c) throws illegalAddressException, DeviceUnavailable {
		// return from  Subroutine  
		// pop the lower  order PC from  stack 
		// pop  high  order PC from  stack
		// 
		
			byte cpstatus = c.bus.read(c.sp);
			c.sp++;
			setFlags(c, cpstatus);
			int lower = c.bus.read(c.sp);
			c.sp++;
			int upper = c.bus.read(c.sp);
			c.sp++;
				
		    
			int newpc =( (upper  &  0xff) << 8) + (int)(lower & 0x00ff);
			c.pc = newpc;
		}
}
