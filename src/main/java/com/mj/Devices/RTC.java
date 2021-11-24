package com.mj.Devices;

import java.text.SimpleDateFormat;
import java.util.Calendar;
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
	private AddressRange mr = new AddressRange(0xa000, 0xa000 + 21);
	private char[] time = new char[mr.size()];
	PBus bus;
	public RTC(PBus bus) {
		this.bus = bus;
		now();
	}

	public DEVTYPE getDeviceType() {
		// TODO Auto-generated method stub
		return DEVTYPE.CHAR;
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
		// TODO Auto-generated method stub
		int localAddress = address - mr.startAddressRange;
		readLock.lock();
		byte value = (byte) time[localAddress];
		logger.debug(String.format("Address %04x Virtual Address %04x Value %02x", localAddress, address, value));
		readLock.unlock();
		return value;
	}

	@Override
	public void raiseInterrupt() {
		// TODO Auto-generated method stub

	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
		while (true) {
			now();
			bus.raiseInterupt();
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}
	}

	public void now() {
		final String DATE_FORMAT_NOW = "yyyy-MM-dd HH:mm:ss";
		Calendar cal = Calendar.getInstance();
		SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT_NOW);
		String tmp = sdf.format(cal.getTime());
		readLock.lock();
		for (int i = 0; i < tmp.length(); i++) {
			time[i] = tmp.trim().charAt(i);
			
		}
		readLock.unlock();
		String t = new String(time);
		logger.debug("RTC-NOW: " + t);
		
	}

	@Override
	public void reset() {
		// TODO Auto-generated method stub
		
	}
}
