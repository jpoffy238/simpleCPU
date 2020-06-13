package com.mj.Firmware.Stack;

import com.mj.Firmware.Framework.Instruction;
import com.mj.cpu001.CPU;
import com.mj.exceptions.DeviceUnavailable;
import com.mj.exceptions.cflagException;
import com.mj.exceptions.illegalAddressException;
import com.mj.exceptions.nflagException;
import com.mj.exceptions.zflagException;

public class PLA extends Instruction {

	/*
	 * PLA - Pull Accumulator
Pulls an 8 bit value from the stack and into the accumulator. The zero and negative flags are set as appropriate.

C	Carry Flag	Not affected
Z	Zero Flag	Set if A = 0
I	Interrupt Disable	Not affected
D	Decimal Mode Flag	Not affected
B	Break Command	Not affected
V	Overflow Flag	Not affected
N	Negative Flag	Set if bit 7 of A is set
Addressing Mode	
Opcode
Bytes
Cycles
Implied	
$68
1
4

	 */
	public PLA() {
	
			super((byte)(0x68));
			setProperty(KEY_MNEMONIC, "PLA");
			setProperty(KEY_ADDRESSING_MODE, VALUE_ADDM_IMP);
			setProperty(KEY_OPCODE, "0x68");
			setProperty(KEY_INSTRUCTION_SIZE, "1");
			setProperty(KEY_CYCLES, "4");
			setProperty(KEY_WEB,"http://6502.org/tutorials/6502opcodes.html#PLA" );
			setProperty(KEY_DESCRIPTION ,"Pulls an 8 bit value from the stack and into the accumulator. The zero and negative flags are set as appropriate");
		
		
	}
	public void exeute(CPU c) throws illegalAddressException, DeviceUnavailable {
		// TODO Auto-generated method stub
			
		try {	
			c.a.set(c.bus.read(c.sp));
		} catch (zflagException z) {
			c.ZFLAG.set();
		} catch (nflagException e) {
			// TODO Auto-generated catch block
			c.NFLAG.set();
		}
		
		c.sp++;
		
		
	

	}



}
