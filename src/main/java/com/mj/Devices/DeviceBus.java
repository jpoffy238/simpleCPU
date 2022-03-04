package com.mj.Devices;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.mj.exceptions.DeviceUnavailable;
import com.mj.exceptions.ROException;
import com.mj.exceptions.illegalAddressException;

public class DeviceBus implements PBus {
	private Lock readLock = new ReentrantLock();
	String formater = "DeviceType : %s10 : BusId : %s10 : Object : %s20";
	private static final Logger logger = LogManager.getLogger(DeviceBus.class);
	private Map<AddressRange, Device> devices = new HashMap<AddressRange, Device>();
	protected boolean interuptRaised = false;
	protected boolean raiseNMInterupt = false;
	protected boolean powerOnReset = false;
	public void write(int address, byte data) throws ROException, illegalAddressException, DeviceUnavailable {
		
		logger.trace("WRITE: " + String.format("%04x %02x: " , address, data));
		Device d = getDevice(address);
		if (null == d) {
			throw new DeviceUnavailable(address);
		}
		logger.trace ( String.format(formater,  d.getDeviceType().name(), d.getBusId().name(),  d.getClass().getCanonicalName())) ;
		d.write(address, data);

	}

	public byte read(int destAddress) throws illegalAddressException, DeviceUnavailable  {
		
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
		
		
		return interuptRaised;
	}

	public boolean IsNMInterruptRaised() {
	
		return raiseNMInterupt;
	}

	public boolean IsPowerOnResetRased() {
		
		return powerOnReset;
	}

	public void write(BussId bus, int address, byte data)
			throws ROException, illegalAddressException, DeviceUnavailable {
		

	}

	public byte read(BussId bus, int destAddress) throws illegalAddressException, DeviceUnavailable {
	
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
		readLock.lock();
		Device d = null;
		Set<AddressRange> s = devices.keySet();
		for ( AddressRange r : s) {
			logger.trace(r.toString());
			
			if (r.contains(address) ) {
				d = devices.get(r);
				logger.trace ( String.format("Device Match: " + formater,  d.getDeviceType().name(), d.getBusId().name(),  d.getClass().getCanonicalName())) ;
				break;
			}
		}
		readLock.unlock();
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

	@Override
	public void startDevices() {
		Device d = null;
		Set<AddressRange> s = devices.keySet();
		for ( AddressRange r : s) {
			d = devices.get(r);
			if (d instanceof Thread) {
				((Thread) d).start();
			}
	}
	}
	public void stopDevices() {
		Device d = null;
		Set<AddressRange> s = devices.keySet();
		for ( AddressRange r : s) {
			d = devices.get(r);
			if (d instanceof Thread) {
				((Thread) d).interrupt();
			}
	}
	}
	@Override
	public void resetDevice() {
		// TODO Auto-generated method stub
		
	}

	public List<Device> getDevices() {
		// TODO Auto-generated method stub
		return null;
	}
	

	
	

}
