package Firmeware.Load;

import Firmeware.Framework.Instruction;
import cpu001.CPU;
import exceptions.DeviceUnavailable;
import exceptions.illegalAddressException;
import exceptions.nflagException;
import exceptions.zflagException;

public class LDXABSY extends Instruction {
	public LDXABSY() {
		super((byte) 0xbe);
		setProperty(KEY_MNEMONIC, "LDX");
		setProperty(KEY_ADDRESSING_MODE, VALUE_ADDM_ABY);
		setProperty(KEY_OPCODE, "0xbe");
		setProperty(KEY_INSTRUCTION_SIZE, "3");
		setProperty(KEY_CYCLES, "4");
		setProperty(KEY_FLAGS_EFFECTED, "Z,N");
		setProperty(KEY_WEB,"http://www.obelisk.me.uk/index.html" );
		setProperty(KEY_DESCRIPTION, "Loads a byte of memory from opperand"
				+ " into the X register setting the zero and negative flags as appropriate.");

	}
	public void exeute(CPU c) throws illegalAddressException, DeviceUnavailable {
		// TODO Auto-generated method stub
		byte opperand_lower = c.memory.read(c.pc); 
		c.pc++;
		byte opperand_upper = c.memory.read(c.pc); 
		c.pc++;
		int loadAddress = opperand_upper << 8 + opperand_lower + c.y.get();
		
		byte value = c.memory.read((int)(loadAddress));
		try {
			c.x.set(value);
		} catch (zflagException e) {
			c.ZFLAG.set();
		} catch (nflagException e) {
			c.NFLAG.set();
		}
	
}
}
