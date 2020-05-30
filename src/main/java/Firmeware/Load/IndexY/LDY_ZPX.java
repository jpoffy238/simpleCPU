package Firmeware.Load.IndexY;

import Firmeware.Framework.Instruction;
import cpu001.CPU;
import exceptions.DeviceUnavailable;
import exceptions.illegalAddressException;
import exceptions.nflagException;
import exceptions.zflagException;

public class LDY_ZPX extends Instruction {
/*
 *  Loads X register from zero page
 */
	public LDY_ZPX() {
		super((byte) 0xb4);
		setProperty(KEY_MNEMONIC, "LDY");
		setProperty(KEY_ADDRESSING_MODE, VALUE_ADDM_ZPX);
		setProperty(KEY_OPCODE, "0xb4");
		setProperty(KEY_INSTRUCTION_SIZE, "2");
		setProperty(KEY_CYCLES, "4");
		setProperty(KEY_FLAGS_EFFECTED, "Z,N");
		setProperty(KEY_WEB,"http://www.obelisk.me.uk/index.html" );
		setProperty(KEY_DESCRIPTION, "Loads a byte of memory from zero page (opperand+Y) "
				+ " into the X register setting the zero and negative flags as appropriate.");

	}
	public void exeute(CPU c) throws illegalAddressException, DeviceUnavailable {
		int zeroPageAddress = getZeroPageXAddress(c); // will wrap around zero page 
		int value = c.memory.read(zeroPageAddress);
		try {
			c.y.set(value);
		} catch (zflagException e) {
			handleZException(c);
		} catch (nflagException e) {
			handleNException(c);
		}
	
		c.pc++;
	}
}
