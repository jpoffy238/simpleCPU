package com.mj.Devices;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.mj.exceptions.DeviceUnavailable;

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

}
