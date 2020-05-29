package Firmeware.ExecutionFlow;

import Firmeware.Framework.Instruction;
import cpu001.CPU;

public class NOP extends Instruction  {
/*
 * NOP (No OPeration)

Affects Flags: none

MODE           SYNTAX       HEX LEN TIM
Implied       NOP           $EA  1   2

NOP is used to reserve space for future modifications or effectively REM out existing code. 
 * 
 */
	public NOP() {
		super((byte)0xea);
		setProperty(KEY_MNEMONIC, "NOP");
		setProperty(KEY_ADDRESSING_MODE, VALUE_ADDM_IMP);
		setProperty(KEY_OPCODE, "0xEA");
		setProperty(KEY_INSTRUCTION_SIZE, "1");
		setProperty(KEY_CYCLES, "2");
		setProperty(KEY_WEB,"http://6502.org/tutorials/6502opcodes.html#NOP" );
		setProperty(KEY_DESCRIPTION ,"NOP is used to reserve space for future modifications or effectively REM out existing code");
	}
		public void exeute(CPU c) {
		// TODO Auto-generated method stub
	
	}

	public void setFlags(CPU u) {
		// TODO Auto-generated method stub

	}

}
