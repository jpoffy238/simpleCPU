package com.mj.Devices;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.mj.exceptions.DeviceUnavailable;
import com.mj.exceptions.ROException;
import com.mj.exceptions.illegalAddressException;

public class DeviceBus implements PBus {
	private Map<MemoryRange, Device> devices = new HashMap<MemoryRange, Device>();
	
	public void write(int address, byte data) throws ROException, illegalAddressException, DeviceUnavailable {
		// TODO Auto-generated method stub
		Device d = getDevice(address);
		d.write(address, data);

	}

	public byte read(int destAddress) throws illegalAddressException, DeviceUnavailable  {
		// TODO Auto-generated method stub
		Device d = getDevice(destAddress);
		byte value = d.read(destAddress);
		return value;
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

	public void registerDevice(Device deviceHandler) {
		// TODO Auto-generated method stub
		if (! devices.containsKey(deviceHandler.getAddressRange())) {
			devices.put(deviceHandler.getAddressRange(), deviceHandler);
			
		}
	}

	public void unregisterDevice(Device deviceHandler) {
		// TODO Auto-generated method stub

	}

	private Device getDevice(int address) {
		Device returnDevice = null;
		Set<MemoryRange> s = devices.keySet();
		for ( MemoryRange r : s) {
			if (r.contains(address) ) {
				returnDevice = devices.get(r);
				break;
			}
		}
		return returnDevice;
	}

	
	

}
