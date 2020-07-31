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

public class ConsoleDevice implements charDevice {
	Queue<Integer> output = new LinkedList<Integer>();
	Queue<Integer> input = new LinkedList<Integer>();
	final  Logger logger = LogManager.getLogger(ConsoleDevice.class);
	PBus sysBus;
	public ConsoleDevice(PBus SystemBus) {
		sysBus = SystemBus;
	}
	public void write(int data) throws DeviceUnavailable {
		// TODO Auto-generated method stub
		System.out.append((char)data);
	}

	public byte read1()  {
		// TODO Auto-generated method stub
		byte returnValue = 0;
		if (input.size() > 0) {
			returnValue = (byte)(input.remove().byteValue());
		
		} 
		return returnValue;
	}

	public byte status1() throws DeviceUnavailable {
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

	public AddressRange getAddressRange() {
		// TODO Auto-generated method stub
		return new AddressRange(0xe000, 0xe001);
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
			return read1();
		} else {
			logger.debug("Reading status Control   : " );
			return status1();
		}
	}

	public void raiseInterrupt() {
		// TODO Auto-generated method stub
		sysBus.raiseInterupt();
	}
	@Override
	public int read() throws DeviceUnavailable {
		// TODO Auto-generated method stub
		return 0;
	}
	@Override
	public int status() throws DeviceUnavailable {
		// TODO Auto-generated method stub
		return 0;
	}
	@Override
	public void control(int data) throws DeviceUnavailable {
		// TODO Auto-generated method stub
		
	}

}
