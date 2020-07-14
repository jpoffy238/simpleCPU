package com.mj.Devices;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.mj.exceptions.DeviceUnavailable;
import com.mj.exceptions.ROException;
import com.mj.exceptions.illegalAddressException;

public class DeviceBus implements PBus {
	String formater = "DeviceType : %s10 : BusId : %s10 : Object : %s20";
	private static final Logger logger = LogManager.getLogger(DeviceBus.class);
	private Map<MemoryRange, Device> devices = new HashMap<MemoryRange, Device>();
	protected boolean interuptRaised = false;
	protected boolean raiseNMInterupt = false;
	protected boolean powerOnReset = false;
	public void write(int address, byte data) throws ROException, illegalAddressException, DeviceUnavailable {
		// TODO Auto-generated method stub
		logger.trace("WRITE: " + String.format("%04x %02x: " , address, data));
		Device d = getDevice(address);
		if (null == d) {
			throw new DeviceUnavailable(address);
		}
		logger.trace ( String.format(formater,  d.getDeviceType().name(), d.getBusId().name(),  d.getClass().getCanonicalName())) ;
		d.write(address, data);

	}

	public byte read(int destAddress) throws illegalAddressException, DeviceUnavailable  {
		// TODO Auto-generated method stub
		logger.trace("READ: " + String.format("%04x" , destAddress));
		Device d = getDevice(destAddress);
		if (null == d) {
			throw new DeviceUnavailable(destAddress);
		}
		logger.trace ( String.format(formater,  d.getDeviceType().name(), d.getBusId().name(),  d.getClass().getCanonicalName())) ;
		byte value = d.read(destAddress);
		return value;
	}

	public boolean IsInterruptRaised() {
		// TODO Auto-generated method stub
		
		return interuptRaised;
	}

	public boolean IsNMInterruptRaised() {
		// TODO Auto-generated method stub
		return raiseNMInterupt;
	}

	public boolean IsPowerOnResetRased() {
		// TODO Auto-generated method stub
		return powerOnReset;
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
		interuptRaised = true;
	}

	public void raiseNMInterupt() {
		// TODO Auto-generated method stub
		raiseNMInterupt  = true;
	}

	public void raisepowerOnReset() {
		// TODO Auto-generated method stub
		powerOnReset = true;
	}

	public void registerDevice(Device deviceHandler) {
		// TODO Auto-generated method stub
		if (! devices.containsKey(deviceHandler.getAddressRange())) {
			devices.put(deviceHandler.getAddressRange(), deviceHandler);
			
		}
	}

	public void unregisterDevice(Device deviceHandler) {
		// TODO Auto-generated method stub

					logger.trace("Unregister Device " + deviceHandler.getClass().getCanonicalName());
	}

	private Device getDevice(int address) {
		Device d = null;
		Set<MemoryRange> s = devices.keySet();
		for ( MemoryRange r : s) {
			logger.trace(r.toString());
			
			if (r.contains(address) ) {
				d = devices.get(r);
				logger.trace ( String.format("Device Match: " + formater,  d.getDeviceType().name(), d.getBusId().name(),  d.getClass().getCanonicalName())) ;
				break;
			}
		}
		return d;
	}

	@Override
	public void clearInterupt() {
		// TODO Auto-generated method stub
		interuptRaised = false;
	}

	@Override
	public void clearNMInterupt() {
		// TODO Auto-generated method stub
		raiseNMInterupt = false;
	}

	@Override
	public void clearpowerOnReset() {
		// TODO Auto-generated method stub
		powerOnReset = false;
	}

	

	
	

}
