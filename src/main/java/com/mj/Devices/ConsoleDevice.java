package com.mj.Devices;

import java.io.IOException;
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
	AddressRange range = null;
	public ConsoleDevice(PBus SystemBus,AddressRange range ) {
		sysBus = SystemBus;
		this.range= range;
	}
	public void write(int data) throws DeviceUnavailable {
		// TODO Auto-generated method stub

		System.out.append((char)data);

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
		return range;
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
		byte returnValue = 0;
		if ((address & 0x0001) == 0) {
			logger.debug("Reading from file ");
			try {
				returnValue =  (byte) System.console().reader().read();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} 
		return returnValue;
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
