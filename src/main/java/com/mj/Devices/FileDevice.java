package com.mj.Devices;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.mj.Devices.PBus.BussId;
import com.mj.Devices.PBus.DEVTYPE;
import com.mj.exceptions.DeviceUnavailable;
import com.mj.exceptions.ROException;
import com.mj.exceptions.illegalAddressException;

public class FileDevice implements Device {
	final  Logger logger = LogManager.getLogger("FileDevice");
	public void write(int data) throws DeviceUnavailable {
		// TODO Auto-generated method stub
		System.out.print((char)data);
	}

	public int read() throws DeviceUnavailable {
		// TODO Auto-generated method stub
		return 0;
	}

	public int status() throws DeviceUnavailable {
		// TODO Auto-generated method stub
		return 0;
	}

	public void status(byte controlWord) throws DeviceUnavailable {
		// TODO Auto-generated method stub
		logger.debug("STatus control Word = " + controlWord);
	}

	

	public void status(int data) throws DeviceUnavailable {
		// TODO Auto-generated method stub
		logger.debug("STatus control Word = " + data);
	}

	public DEVTYPE getDeviceType() {
		// TODO Auto-generated method stub
		return null;
	}

	public BussId getBusId() {
		// TODO Auto-generated method stub
		return null;
	}

	public MemoryRange getAddressRange() {
		// TODO Auto-generated method stub
		return null;
	}

	public void write(int address, byte data) throws illegalAddressException, ROException, DeviceUnavailable {
		// TODO Auto-generated method stub
		
	}

	public byte read(int address) throws illegalAddressException, ROException, DeviceUnavailable {
		// TODO Auto-generated method stub
		return 0;
	}

}
