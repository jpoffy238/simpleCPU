package com.mj.Firmware.ExecutionFlow;

import com.mj.Firmware.Framework.Instruction;
import com.mj.cpu001.CPU;
import com.mj.exceptions.DeviceUnavailable;
import com.mj.exceptions.illegalAddressException;

public class BRK extends Instruction {
/* 
 * Affect Flags: none

All branches are relative mode and have a length of two bytes. Syntax is "Bxx Displacement" or (better) "Bxx Label". See the notes on the Program Counter for more on displacements.

Branches are dependant on the status of the flag bits when the op code is encountered. A branch not taken requires two machine cycles. Add one if the branch is taken and add one more if the branch crosses a page boundary.

MNEMONIC                       HEX
BPL (Branch on PLus)           $10
BMI (Branch on MInus)          $30
BVC (Branch on oVerflow Clear) $50
BVS (Branch on oVerflow Set)   $70
BCC (Branch on Carry Clear)    $90
BCS (Branch on Carry Set)      $B0
BNE (Branch on Not Equal)      $D0
BEQ (Branch on EQual)          $F0

There is no BRA (BRanch Always) instruction but it can be easily emulated by branching on the basis of a known condition. One of the best flags to use for this purpose is the oVerflow which is unchanged by all but addition and subtraction operations.
A page boundary crossing occurs when the branch destination is on a different page than the instruction AFTER the branch instruction. For example:

  SEC
  BCS LABEL
  NOP
A page boundary crossing occurs (i.e. the BCS takes 4 cycles) when (the address of) LABEL and the NOP are on different pages. This means that
        CLV
        BVC LABEL
  LABEL NOP
the BVC instruction will take 3 cycles no matter what address it is located at.
 */
	public BRK() {
		super((byte)(0x00));
		setProperty(KEY_MNEMONIC, "BRK");
		setProperty(KEY_ADDRESSING_MODE, VALUE_ADDM_REL);
		setProperty(KEY_OPCODE, "0xd0");
		setProperty(KEY_INSTRUCTION_SIZE, "1");
		setProperty(KEY_CYCLES, "7");
		setProperty(KEY_FLAGS_EFFECTED, "NONE");
		setProperty(KEY_WEB,"http://6502.org/tutorials/6502opcodes.html#BRK" );
		setProperty(KEY_DESCRIPTION, "BCC  (Branch on Carry NOT set )  If Carry NOT flag is set");
	
	}
	public void exeute(CPU c) throws illegalAddressException, DeviceUnavailable {
		// TODO Auto-generated method stub
		int upper = (((c.pc) & 0xff00) >> 8);
		int lower = (((c.pc) & 0x00ff));
		c.sp--;
		c.memory.write(c.sp, (byte) (upper & 0xff));
		c.sp--;
		c.memory.write(c.sp, (byte) (lower & 0xff));
		c.sp--;
		c.memory.write(c.sp, psr(c));

		int IntAddress = getInterruptHandlerAddress(c);
		c.pc = IntAddress;
		}
	

	

}
