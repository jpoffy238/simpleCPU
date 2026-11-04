package com.mj.Devices;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.mj.Devices.PBus.BussId;
import com.mj.Devices.PBus.DEVTYPE;
import com.mj.exceptions.DeviceUnavailable;
import com.mj.exceptions.ROException;
import com.mj.exceptions.illegalAddressException;

public class RTC extends Thread implements Device {
	private Lock readLock = new ReentrantLock();
	static Logger logger = LogManager.getLogger(RTC.class);
	private AddressRange mr = new AddressRange(0xa000, 0xa003);
	// Big-endian u_time: seconds since the Unix epoch (UTC), updated once a second.
	// Calendar/display conversion is firmware's job, not this device's.
	private byte[] time = new byte[mr.size()];
	VIA6522 via;

	public RTC(VIA6522 via) {
		this.via = via;
		now();
	}

	public DEVTYPE getDeviceType() {
		return DEVTYPE.BLOCK;
	}

	@Override
	public BussId getBusId() {
		// TODO Auto-generated method stub
		return BussId.DEVICE;
	}

	@Override
	public AddressRange getAddressRange() {
		// TODO Auto-generated method stub
		return mr;
	}

	@Override
	public void write(int address, byte data) throws illegalAddressException, ROException, DeviceUnavailable {
		// TODO Auto-generated method stub
		throw new ROException();
	}

	@Override
	public byte read(int address) throws illegalAddressException, DeviceUnavailable {
		int localAddress = address - mr.startAddressRange;
		readLock.lock();
		byte value = time[localAddress];
		readLock.unlock();
		if (logger.isTraceEnabled()) {
			logger.trace(String.format("Address %04x Virtual Address %04x Value %02x", localAddress, address, value));
		}
		return value;
	}

	@Override
	public void raiseInterrupt() {
		// TODO Auto-generated method stub

	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
		
		while (!isInterrupted()) {
			now();
			via.requestInterrupt(VIA6522.FLAG_T1);
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				return;	
			}

		}
	}

	public void now() {
		int epochSeconds = (int) (System.currentTimeMillis() / 1000L);
		readLock.lock();
		time[0] = (byte) (epochSeconds >>> 24);
		time[1] = (byte) (epochSeconds >>> 16);
		time[2] = (byte) (epochSeconds >>> 8);
		time[3] = (byte) epochSeconds;
		readLock.unlock();

		if (logger.isTraceEnabled()) {
			logger.trace(String.format("RTC-NOW: %08x", epochSeconds));
		}
	}

	@Override
	public void reset() {
		// TODO Auto-generated method stub
		
	}
}
