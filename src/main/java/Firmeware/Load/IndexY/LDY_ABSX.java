package Firmeware.Load.IndexY;

import Firmeware.Framework.Instruction;
import cpu001.CPU;
import exceptions.DeviceUnavailable;
import exceptions.illegalAddressException;
import exceptions.nflagException;
import exceptions.zflagException;

public class LDY_ABSX extends Instruction {
	public LDY_ABSX() {
		super((byte) 0xbc);
		setProperty(KEY_MNEMONIC, "LDY");
		setProperty(KEY_ADDRESSING_MODE, VALUE_ADDM_ABX);
		setProperty(KEY_OPCODE, "0xbc");
		setProperty(KEY_INSTRUCTION_SIZE, "3");
		setProperty(KEY_CYCLES, "4");
		setProperty(KEY_FLAGS_EFFECTED, "Z,N");
		setProperty(KEY_WEB,"http://www.obelisk.me.uk/index.html" );
		setProperty(KEY_DESCRIPTION, "Loads a byte of memory from opperand"
				+ " into the X register setting the zero and negative flags as appropriate.");

	}
	public void exeute(CPU c) throws illegalAddressException, DeviceUnavailable {
		// TODO Auto-generated method stub
		int loadAddress = getAbsoluteAddressY(c);				
		byte value = c.memory.read((int)(loadAddress));
		try {
			c.x.set(value);
		} catch (zflagException e) {
			handleZException(c);
		} catch (nflagException e) {
			handleNException(c);
		}
		c.pc+=2;
	
}
}
