package Firmeware.Load.Accumulator;

import Firmeware.Framework.Instruction;
import cpu001.CPU;
import exceptions.DeviceUnavailable;
import exceptions.illegalAddressException;
import exceptions.nflagException;
import exceptions.zflagException;

public class LDA_ZP extends Instruction {
	// Zero Page reference (X)
	public LDA_ZP() {
		super ((byte)0xa5);
		setProperty(KEY_MNEMONIC, "LDA");
		setProperty(KEY_ADDRESSING_MODE, VALUE_ADDM_ZP);
		setProperty(KEY_OPCODE, "0xa5");
		setProperty(KEY_INSTRUCTION_SIZE, "2");
		setProperty(KEY_CYCLES, "2");
		setProperty(KEY_FLAGS_EFFECTED, "Z,N");
		setProperty(KEY_WEB,"http://www.obelisk.me.uk/6502/reference.html#LDA" );
		setProperty(KEY_DESCRIPTION, "A,Z,N = M - Loads a byte of memory (zeropage) into the accumulator setting the zero and negative flags as appropriate.");
	}
	public void exeute(CPU c) throws illegalAddressException, DeviceUnavailable {
		// TODO Auto-generated method stub
		int address = getZeroPageAddress(c);
		byte m = c.memory.read(address);
		try {
			c.a.set(m);
		} catch (zflagException e) {
			handleZException(c);
		} catch (nflagException e) {
			handleNException(c);
		}
		c.pc++;
	}
}