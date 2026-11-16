package com.mj.Firmware.ExecutionFlow;

import com.mj.Firmware.Framework.Instruction;
import com.mj.cpu001.CPU;
import com.mj.exceptions.DeviceUnavailable;
import com.mj.exceptions.ROException;
import com.mj.exceptions.illegalAddressException;

public class BRK extends Instruction {
/*
 * BRK (BReaK)
Affects Flags: B

MODE           SYNTAX       HEX LEN TIM
Implied       BRK           $00  1   7

BRK causes a software interrupt: it pads itself with a second
(ignored) byte, pushes PC+2, then the status register with the B
flag set, sets the I flag, and jumps through the IRQ/NMI vector -
the same sequence a hardware interrupt uses, just software-
triggered. RTI is the usual way to return from it.
 */
	public BRK() {
		super((byte)(0x00));
		setProperty(KEY_MNEMONIC, "BRK");
		setProperty(KEY_ADDRESSING_MODE, VALUE_ADDM_IMP);
		setProperty(KEY_OPCODE, "0x00");
		setProperty(KEY_INSTRUCTION_SIZE, "1");
		setProperty(KEY_CYCLES, "7");
		setProperty(KEY_FLAGS_EFFECTED, "B");
		setProperty(KEY_WEB,"http://6502.org/tutorials/6502opcodes.html#BRK" );
		setProperty(KEY_DESCRIPTION, "BRK (BReaK) - forces a software interrupt, pushing PC+2 and the status register (B flag set) before jumping through the IRQ/NMI vector.");

	}
	public void exeute(CPU c) throws illegalAddressException, DeviceUnavailable, ROException {
		
		// BRK has a padding byte: the return address is the BRK address + 2
		int ret = (c.pc + 1) & 0xffff;
		c.push((byte) ((ret >> 8) & 0xff));
		c.push((byte) (ret & 0xff));
		c.push(psr(c, true));   // software interrupt: B bit set
		c.IFLAG.set();

		int IntAddress = getInterruptHandlerAddress(c);
		c.pc = IntAddress;
		}
	

	

}
