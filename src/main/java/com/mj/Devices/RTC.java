package com.mj.Devices;

import java.text.SimpleDateFormat;
import java.util.Calendar;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.mj.Devices.PBus.BussId;
import com.mj.Devices.PBus.DEVTYPE;
import com.mj.exceptions.DeviceUnavailable;
import com.mj.exceptions.ROException;
import com.mj.exceptions.illegalAddressException;

public class RTC extends Thread implements Device {
	static Logger logger = LogManager.getLogger(RTC.class);
	private MemoryRange mr = new MemoryRange(0xa000, 0xa000 + 21);
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
	public MemoryRange getAddressRange() {
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
		byte value = (byte) time[localAddress];
		logger.debug(String.format("Address %04x Virtual Address %04x Value %02x", localAddress, address, value));

		return (byte) time[localAddress];
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
				Thread.sleep(10000);
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
		for (int i = 0; i < tmp.length(); i++) {
			time[i] = tmp.trim().charAt(i);

		}
	}
}
