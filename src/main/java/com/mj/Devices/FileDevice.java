package com.mj.Devices;

import java.util.LinkedList;
import java.util.Queue;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.mj.Devices.PBus.BussId;
import com.mj.Devices.PBus.DEVTYPE;
import com.mj.exceptions.DeviceUnavailable;
import com.mj.exceptions.ROException;
import com.mj.exceptions.illegalAddressException;

public class FileDevice implements Device {
	Queue<Integer> output = new LinkedList<Integer>();
	Queue<Integer> input = new LinkedList<Integer>();
	final  Logger logger = LogManager.getLogger(FileDevice.class);
	PBus sysBus;
	public FileDevice(PBus SystemBus) {
		sysBus = SystemBus;
	}
	public void write(int data) throws DeviceUnavailable {
		// TODO Auto-generated method stub
		logger.debug("OUTPUT CHAR : " + (char)data);
	}

	public byte read() throws DeviceUnavailable {
		// TODO Auto-generated method stub
		byte returnValue = 0;
		if (input.size() > 0) {
			returnValue = (byte)(input.remove().byteValue());
			return returnValue;
		} else {
			throw new DeviceUnavailable();
		}
	
	}

	public byte status() throws DeviceUnavailable {
		// TODO Auto-generated method stub
		int size = input.size();
		if (size > 127) {
			size = 127;
		}
		return (byte)(size & 0x00ff);
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
		return DEVTYPE.CHAR;
	}

	public BussId getBusId() {
		// TODO Auto-generated method stub
		return BussId.DEVICE;
	}

	public MemoryRange getAddressRange() {
		// TODO Auto-generated method stub
		return new MemoryRange(0xe000, 0xe0001);
	}

	public void write(int address, byte data) throws illegalAddressException, ROException, DeviceUnavailable {
		if ((address & 0x0001) == 0) {
			logger.debug("OUTPUT CHAR : " + (char)data);
			write(data);
		} else {
			logger.debug("Control   : " + data);
			status(data);
		}
		
		
	}

	public byte read(int address) throws illegalAddressException,  DeviceUnavailable {
		// TODO Auto-generated method stub
		if ((address & 0x0001) == 0) {
			logger.debug("Reading from file ");
			return read();
		} else {
			logger.debug("Reading status Control   : " );
			return status();
		}
	}

	public void raiseInterrupt() {
		// TODO Auto-generated method stub
		sysBus.raiseInterupt();
	}

}
