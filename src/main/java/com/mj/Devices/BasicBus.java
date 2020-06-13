package com.mj.Devices;

import com.mj.exceptions.DeviceUnavailable;
import com.mj.exceptions.ROException;
import com.mj.exceptions.illegalAddressException;
import com.mj.memoryInterface.MemoryDriver;
import com.mj.memoryInterface.basicMemory;

public class BasicBus implements CPUBus {
	public BasicBus() {
	try {
		m.write(0xfffc, (byte)0x00);
		m.write(0xfffd, (byte)0x10);
		m.write(0xfffe,(byte)0x00);
		m.write(0xffff,(byte)0x10);
		m.write(0xfffa,(byte)0x00);
		m.write(0xfffb, (byte)0x10);
	} catch (illegalAddressException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	} catch (ROException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
	}
	MemoryDriver m  = new basicMemory();
	boolean interrupt = false;
	boolean NMInterupt = false;
	boolean powerOnReset = true;
	public void write(int address, byte data) throws ROException, illegalAddressException, DeviceUnavailable {
		// TODO Auto-generated method stub
		m.write(address, data);
	}

	public byte read(int destAddress) throws illegalAddressException, DeviceUnavailable {
		// TODO Auto-generated method stub
		return m.read(destAddress);
	}

	public boolean IsInterruptRaised() {
		// TODO Auto-generated method stub
		boolean value = interrupt;
		interrupt = false;
		return value;
	}

	public boolean IsNMInterruptRaised() {
		// TODO Auto-generated method stub
		boolean value = NMInterupt;
		NMInterupt = false;
		return value;
	}

	public boolean IsPowerOnResetRased() {
		// TODO Auto-generated method stub
		boolean value = powerOnReset;
		powerOnReset = false;
		return value;
	}

}
