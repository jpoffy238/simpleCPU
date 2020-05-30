package Firmeware.Load;

import Firmeware.Framework.Instruction;
import cpu001.CPU;
import exceptions.DeviceUnavailable;
import exceptions.illegalAddressException;
import exceptions.nflagException;
import exceptions.zflagException;

public class LDXZP extends Instruction {
/*
 *  Loads X register from zero page
 */
	public LDXZP() {
		super((byte) 0xa6);
		setProperty(KEY_MNEMONIC, "LDX");
		setProperty(KEY_ADDRESSING_MODE, VALUE_ADDM_ZP);
		setProperty(KEY_OPCODE, "0xa6");
		setProperty(KEY_INSTRUCTION_SIZE, "2");
		setProperty(KEY_CYCLES, "3");
		setProperty(KEY_FLAGS_EFFECTED, "Z,N");
		setProperty(KEY_WEB,"http://www.obelisk.me.uk/index.html" );
		setProperty(KEY_DESCRIPTION, "Loads a byte of memory from zero page into the X register setting the zero and negative flags as appropriate.");

	}
	public void exeute(CPU c) throws illegalAddressException, DeviceUnavailable {
		// TODO Auto-generated method stub
		byte m = c.memory.read(c.pc); // read the zero page address - operand 
		byte value = c.memory.read((int)(m &0x00ff));
		try {
			c.x.set(value);
		} catch (zflagException e) {
			c.ZFLAG.set();
			c.NFLAG.clear();
		} catch (nflagException e) {
			c.NFLAG.set();
			c.ZFLAG.clear();
		}
	
		c.pc++;
	}
}
