package Firmeware.Framework;

import cpu001.CPU;
import exceptions.DeviceUnavailable;
import exceptions.illegalAddressException;
// Test the flags to byte and byte to flags code.
public class TEST extends Instruction {
	public TEST() {
		super((byte)0x31);
	}
	public void exeute(CPU c) throws illegalAddressException, DeviceUnavailable {
		// Start with all things clear 
		byte currentState = psr(c);
		boolean restore = true;;
		System.out.println("Initial State: " + c.dump());
		byte data = c.memory.read(c.pc);
		if ((data & 0x80) != 0 ) {
			restore = false;
		}
		setFlags(c,data);
		System.out.println("Flags Set : [" + String.format("%02x" , data) + "] " + c.dump());
		if (restore) {
			setFlags(c, currentState);
		}
		System.out.println("Final State : " + c.dump());
		c.pc++;
		
	}
}
