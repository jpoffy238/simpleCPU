package com.mj.Firmware.ExecutionFlow;

import com.mj.Firmware.Framework.Instruction;
import com.mj.cpu001.CPU;
import com.mj.exceptions.DeviceUnavailable;
import com.mj.exceptions.ROException;
import com.mj.exceptions.illegalAddressException;

/*
 * 
JSR (Jump to SubRoutine)
Affects Flags: none

MODE           SYNTAX       HEX LEN TIM
Absolute      JSR $5597     $20  3   6

JSR pushes the address-1 of the next operation on to 
the stack before transferring program control to the 
following address. Subroutines are normally terminated 
by a RTS op code.
 */
public class JSR extends Instruction {
	public JSR() {
		super((byte)(0x20));
		setProperty(KEY_MNEMONIC, "JSR");
		setProperty(KEY_ADDRESSING_MODE, VALUE_ADDM_ABS);
		setProperty(KEY_OPCODE, "0x20");
		setProperty(KEY_INSTRUCTION_SIZE, "3");
		setProperty(KEY_CYCLES, "6");
		setProperty(KEY_WEB,"http://6502.org/tutorials/6502opcodes.html#JSR" );
		setProperty(KEY_DESCRIPTION ,"JSR pushes the address-1 of the next operation " +
				"on to the stack before transferring program control to the following address. Subroutines are normally terminated by a RTS op code.");
	}
	public void exeute(CPU c) throws illegalAddressException, DeviceUnavailable, ROException {
		// Jump to Subroutine  
		// Push the high order PC+2 to stack 
		// Push low order PC+2 to stack
		// JMP to Immediate address next two bytes of this instruction
		// Get the upper and lower PC + 2 (the return address)
		    int upper = (((c.pc+2) & 0xff00 )>> 8); 
		    int  lower = (((c.pc+2) & 0x00ff));
			
		    // push on stack
		    c.sp--;
			c.bus.write(c.sp, (byte)(upper & 0xff));
			c.sp--;
			c.bus.write(c.sp, (byte)(lower & 0xff));
			
			
			// Load new jmp address into PC
		    lower = (c.bus.read(c.pc)  & 0xff);
		    c.pc++;
		    upper = (c.bus.read(c.pc) & 0xff);
			int newpc =( (upper & 0x00ff) << 8) + (int)(lower & 0x00ff);
			c.pc = newpc;
		}
	}

