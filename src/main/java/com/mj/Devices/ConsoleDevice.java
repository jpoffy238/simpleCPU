package com.mj.Devices;

import java.io.Console;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.LinkedList;
import java.util.Queue;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.NonBlockingReader;

import com.mj.Devices.PBus.BussId;
import com.mj.Devices.PBus.DEVTYPE;
import com.mj.exceptions.DeviceUnavailable;
import com.mj.exceptions.ROException;
import com.mj.exceptions.illegalAddressException;

public class ConsoleDevice implements charDevice , Runnable {
	Queue<Integer> output = new LinkedList<Integer>();
	Queue<Integer> input = new LinkedList<Integer>();
	final  Logger logger = LogManager.getLogger(ConsoleDevice.class);
	PBus sysBus;
	Terminal terminal ;
	NonBlockingReader reader;
	PrintWriter writer;
	Console con = null;
	AddressRange range;
	public ConsoleDevice(PBus SystemBus,AddressRange range ) {
		sysBus = SystemBus;
		this.range= range;
		try {
			terminal = TerminalBuilder.builder()
				    .jna(true)
				    .system(true)
				    .build();
			reader = terminal.reader();
			writer = terminal.writer();
			terminal.enterRawMode();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			// raw mode means we get keypresses rather than line buffered input
		}
		
		
	}
	public void write(int data) throws DeviceUnavailable {
		// TODO Auto-generated method stub

		
		writer.append((char) data);

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
				char[] cbuff = new char[10];
				
				int chsread = 0;
				while ( chsread <= 0 ) {
					chsread = System.console().reader().read(cbuff,0, 1);
					Thread.sleep(10);
				}
				returnValue =  (byte) cbuff[0];
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InterruptedException e) {
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
	@Override
	public void run() {
		// TODO Auto-generated method stub
		while (true) {
			
		
		int peekresults = 0;
		try {
			peekresults = reader.peek(10);
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		while ( peekresults > 0) {
			try {
				int inchar = reader.read();
				input.add(new Integer(inchar));
				peekresults = reader.peek(1);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}
	}
	}

}
