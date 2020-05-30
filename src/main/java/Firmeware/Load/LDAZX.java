package Firmeware.Load;

import Firmeware.Framework.Instruction;
import cpu001.CPU;
import exceptions.DeviceUnavailable;
import exceptions.illegalAddressException;
import exceptions.nflagException;
import exceptions.zflagException;

public class LDAZX extends Instruction {
	// Zero Page reference (X)
	public LDAZX() {
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
		int address = getZeroPageXAddress(c);
		byte m = c.memory.read(address);
		try {
			c.a.set(m);
		} catch (zflagException e) {
			c.NFLAG.clear();
			c.ZFLAG.set();
		} catch (nflagException e) {
			c.ZFLAG.clear();
			c.NFLAG.set();
		}
		c.pc = (++c.pc);
	}
}
