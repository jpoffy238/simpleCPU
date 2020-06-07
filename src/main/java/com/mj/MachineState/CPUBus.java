package com.mj.MachineState;

import com.mj.Devices.Device;
import com.mj.MachineCycle.PBus;
import com.mj.cpu001.CPU;
import com.mj.exceptions.DeviceUnavailable;
import com.mj.exceptions.illegalAddressException;
import com.mj.memoryInterface.MemoryDriver;

public class CPUBus implements PBus {
	
	
	
	public void write(int address, byte data) throws illegalAddressException, DeviceUnavailable {
		// TODO Auto-generated method stub
		
	}
	
	public byte read(int destAddress) throws illegalAddressException, DeviceUnavailable {
		// TODO Auto-generated method stub
		return 0;
	}

	public void interupt() {
		// TODO Auto-generated method stub
		
	}

	public void registerDevice( PBus.BussId bus, int address, int length, Device deviceHandler) {
		// TODO Auto-generated  stub
		
	}

	public void unregisterDevice(int address) {
		// TODO Auto-generated method stub
		
	}

	public void blockWrite(int address, byte[] data) throws illegalAddressException, DeviceUnavailable {
		// TODO Auto-generated method stub
		
	}

	public byte[] blockRead(int destAddress) throws illegalAddressException, DeviceUnavailable {
		// TODO Auto-generated method stub
		return null;
	}

	public void raiseInterupt() {
		// TODO Auto-generated method stub
		
	}

	public void raiseNMInterupt() {
		// TODO Auto-generated method stub
		
	}

	public void raisepowerOnReset() {
		// TODO Auto-generated method stub
		
	}
	public void unregisterDevice(BussId bus, int address) {
		// TODO Auto-generated method stub
		
	}

}
