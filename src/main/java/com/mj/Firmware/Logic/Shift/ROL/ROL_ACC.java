package com.mj.Firmware.Logic.Shift.ROL;

import com.mj.Firmware.Framework.Instruction;
import com.mj.cpu001.CPU;
import com.mj.exceptions.DeviceUnavailable;
import com.mj.exceptions.ROException;
import com.mj.exceptions.illegalAddressException;
import com.mj.exceptions.nflagException;
import com.mj.exceptions.zflagException;

public class ROL_ACC extends Instruction {
	/*
	 * Affect Flags: none
	 * 
	 * All branches are relative mode and have a length of two bytes. Syntax is
	 * "Bxx Displacement" or (better) "Bxx Label". See the notes on the Program
	 * Counter for more on displacements.
	 * 
	 * Branches are dependant on the status of the flag bits when the op code is
	 * encountered. A branch not taken requires two machine cycles. Add one if the
	 * branch is taken and add one more if the branch crosses a page boundary.
	 * 
	 * MNEMONIC HEX BPL (Branch on PLus) $10 BMI (Branch on MInus) $30 BVC (Branch
	 * on oVerflow Clear) $50 BVS (Branch on oVerflow Set) $70 BCC (Branch on Carry
	 * Clear) $90 BCS (Branch on Carry Set) $B0 BNE (Branch on Not Equal) $D0 BEQ
	 * (Branch on EQual) $F0
	 * 
	 * There is no BRA (BRanch Always) instruction but it can be easily emulated by
	 * branching on the basis of a known condition. One of the best flags to use for
	 * this purpose is the oVerflow which is unchanged by all but addition and
	 * subtraction operations. A page boundary crossing occurs when the branch
	 * destination is on a different page than the instruction AFTER the branch
	 * instruction. For example:
	 * 
	 * SEC BCS LABEL NOP A page boundary crossing occurs (i.e. the BCS takes 4
	 * cycles) when (the address of) LABEL and the NOP are on different pages. This
	 * means that CLV BVC LABEL LABEL NOP the BVC instruction will take 3 cycles no
	 * matter what address it is located at.
	 */
	public ROL_ACC() {
		super((byte) (0x2a));
		setProperty(KEY_MNEMONIC, "ROL");
		setProperty(KEY_ADDRESSING_MODE, VALUE_ADDM_IMP);
		setProperty(KEY_OPCODE, "0x2a");
		setProperty(KEY_INSTRUCTION_SIZE, "1");
		setProperty(KEY_CYCLES, "2");
		setProperty(KEY_FLAGS_EFFECTED, "NONE");
		setProperty(KEY_WEB, "http://6502.org/tutorials/6502opcodes.html#ROL");
		setProperty(KEY_DESCRIPTION, "Shift A left  by 1 or (A/2). ");

	}

	public void exeute(CPU c) throws illegalAddressException, DeviceUnavailable, ROException {
		// TODO Auto-generated method stub
		c.ZFLAG.clear();
		c.NFLAG.clear();

		int a = c.a.get();
		
		int result = a << 1;
		if (c.CFLAG.isSet()) {
			result += 1; // Pull in Carry Flag if set
			c.CFLAG.clear();  // Clear it
		}
		if ((a & 0x80) != 0) {
			c.CFLAG.set(); 
		} else { 
			if ( result == 0) {
				c.ZFLAG.set();
				c.NFLAG.clear();
			} else { 
				if ((result & 0x80)  == 0x80 ){
					c.NFLAG.set();
					c.ZFLAG.clear();
				}
			}
		
		}
		try {
			c.a.set(result & 0xff);
		} catch (zflagException | nflagException e) {
			// TODO Auto-generated catch block
			
		}
	}

}
