package com.mj.Devices;

import java.text.SimpleDateFormat;
import java.util.Calendar;

import com.mj.Devices.PBus.BussId;
import com.mj.Devices.PBus.DEVTYPE;
import com.mj.exceptions.DeviceUnavailable;
import com.mj.exceptions.ROException;
import com.mj.exceptions.illegalAddressException;

public class RTC extends Thread implements Device {

	public RTC () {
		now();
	}
	private MemoryRange mr = new MemoryRange(0xec02, 0xec22);
	private char[] time = new char[mr.size()];
	@Override
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
		return  mr;
	}

	@Override
	public void write(int address, byte data) throws illegalAddressException, ROException, DeviceUnavailable {
		// TODO Auto-generated method stub
		throw new ROException();
	}

	@Override
	public byte read(int address) throws illegalAddressException, DeviceUnavailable {
		// TODO Auto-generated method stub
		return (byte)time[address - mr.startAddressRange];
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
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			 
		}
	}
	public  void now() {
		final String DATE_FORMAT_NOW = "yyyy-MM-dd HH:mm:ss";
		Calendar cal = Calendar.getInstance();
		SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT_NOW);
		String tmp = sdf.format(cal.getTime());
		for (int i = 0; i < tmp.length(); i++) { 
           time[i] = tmp.trim().charAt(i);
		 
		}
	}
}
