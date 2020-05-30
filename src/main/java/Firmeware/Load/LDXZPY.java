package Firmeware.Load;

import Firmeware.Framework.Instruction;
import cpu001.CPU;
import exceptions.DeviceUnavailable;
import exceptions.illegalAddressException;
import exceptions.nflagException;
import exceptions.zflagException;

public class LDXZPY extends Instruction {
/*
 *  Loads X register from zero page
 */
	public LDXZPY() {
		super((byte) 0xb6);
		setProperty(KEY_MNEMONIC, "LDX");
		setProperty(KEY_ADDRESSING_MODE, VALUE_ADDM_ZPY);
		setProperty(KEY_OPCODE, "0xb6");
		setProperty(KEY_INSTRUCTION_SIZE, "2");
		setProperty(KEY_CYCLES, "4");
		setProperty(KEY_FLAGS_EFFECTED, "Z,N");
		setProperty(KEY_WEB,"http://www.obelisk.me.uk/index.html" );
		setProperty(KEY_DESCRIPTION, "Loads a byte of memory from zero page (opperand+Y) "
				+ " into the X register setting the zero and negative flags as appropriate.");

	}
	public void exeute(CPU c) throws illegalAddressException, DeviceUnavailable {
		// TODO Auto-generated method stub
		byte opperand = c.memory.read(c.pc); // read the zero page address - operand 
		byte zeroPageAddress = (byte)((c.x.get() + opperand)&0x00ff); // will wrap around zero page 
		byte value = c.memory.read((int)(zeroPageAddress &0x00ff));
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
