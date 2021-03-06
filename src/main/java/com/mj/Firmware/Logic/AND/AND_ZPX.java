package com.mj.Firmware.Logic.AND;

import com.mj.Firmware.Framework.Instruction;
import com.mj.cpu001.CPU;
import com.mj.exceptions.DeviceUnavailable;
import com.mj.exceptions.illegalAddressException;
import com.mj.exceptions.nflagException;
import com.mj.exceptions.zflagException;

public class AND_ZPX extends Instruction {
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
	public AND_ZPX() {
		super((byte) (0x35));
		setProperty(KEY_MNEMONIC, "BBIT");
		setProperty(KEY_ADDRESSING_MODE, VALUE_ADDM_REL);
		setProperty(KEY_OPCODE, "0x35");
		setProperty(KEY_INSTRUCTION_SIZE, "3");
		setProperty(KEY_CYCLES, "3");
		setProperty(KEY_FLAGS_EFFECTED, "NONE");
		setProperty(KEY_WEB, "http://6502.org/tutorials/6502opcodes.html#BIT");
		setProperty(KEY_DESCRIPTION, "BCC  (Branch on Carry  set )  If Carry  flag is set");

	}

	public void exeute(CPU c) throws illegalAddressException, DeviceUnavailable {
		// TODO Auto-generated method stub
		int zeroPageAddress = getZeroPageXAddress(c);
		byte testValue = c.bus.read(zeroPageAddress);
		int result = testValue & (byte) (c.a.get() & 0xff);
		try {
			c.ZFLAG.clear();
			c.NFLAG.clear();
			c.a.set(result& 0xff);
		} catch (zflagException e) {
			handleZException(c);
		} catch (nflagException e) {
			handleNException(c);
		}
		c.pc++;
	}

}
