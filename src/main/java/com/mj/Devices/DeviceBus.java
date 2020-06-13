package com.mj.Devices;

import com.mj.exceptions.DeviceUnavailable;
import com.mj.exceptions.ROException;
import com.mj.exceptions.illegalAddressException;

public class DeviceBus implements PBus {

	public void write(int address, byte data) throws ROException, illegalAddressException, DeviceUnavailable {
		// TODO Auto-generated method stub

	}

	public byte read(int destAddress) throws illegalAddressException, DeviceUnavailable {
		// TODO Auto-generated method stub
		return 0;
	}

	public boolean IsInterruptRaised() {
		// TODO Auto-generated method stub
		return false;
	}

	public boolean IsNMInterruptRaised() {
		// TODO Auto-generated method stub
		return false;
	}

	public boolean IsPowerOnResetRased() {
		// TODO Auto-generated method stub
		return false;
	}

	public void write(BussId bus, int address, byte data)
			throws ROException, illegalAddressException, DeviceUnavailable {
		// TODO Auto-generated method stub

	}

	public byte read(BussId bus, int destAddress) throws illegalAddressException, DeviceUnavailable {
		// TODO Auto-generated method stub
		return 0;
	}

	public void blockWrite(BussId bus, int address, byte[] data)
			throws ROException, illegalAddressException, DeviceUnavailable {
		// TODO Auto-generated method stub

	}

	public byte[] blockRead(BussId bus, int destAddress) throws illegalAddressException, DeviceUnavailable {
		// TODO Auto-generated method stub
		return null;
	}

	public void raiseInterupt() {
		// TODO Auto-generated method stub

	}

	public void raisekNMInterupt() {
		// TODO Auto-generated method stub

	}

	public void raisepowerOnReset() {
		// TODO Auto-generated method stub

	}

	public void registerDevice(BussId bus, DEVTYPE t, IOALLOW io, MemoryRange range, Device deviceHandler) {
		// TODO Auto-generated method stub

	}

	public void unregisterDevice(BussId bus, DEVTYPE t, MemoryRange range) {
		// TODO Auto-generated method stub

	}

}
