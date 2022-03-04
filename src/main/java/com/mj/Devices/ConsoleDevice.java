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

public class ConsoleDevice extends Thread implements charDevice {
	// Console Device usage:
	// Console device uses 2 bytes of memory address space:
	// o The even byte (LSb=0) is used to read from OR write to
	// the console device. The read operation can be set to
	// - Block until a key is pressed
	// - NoBlock returns a Null 0x00 character there is
	// no input available
	// - Raise Interrupt.
	// The odd memory location (LSb=1) is the control/status
	// for the console device. Bit definition:
	// Read/Write Operations:
	// Bit 0: Data Available Bit
	// - If 1 then data is available and can be read
	// - If 0 then data is not available
	// Bit 1: Mode Bit
	// - If 1 then Data read will block
	// - If 0 then Data read will not block
	// Bit 2: Interrupt Mode
	// - If 1 then device will raise the interrupt.
	// - If 0 then device will not raise interrupt when data is available.
	// Bit 3: Unused
	// Bit 4-7: (Upper nibble) Number of Characters Available for reading (0-15)
	//
	//
	Queue<Integer> output = new LinkedList<Integer>();
	Queue<Integer> input = new LinkedList<Integer>();
	final Logger logger = LogManager.getLogger(ConsoleDevice.class);
	int status = 0;
	PBus sysBus;
	Terminal terminal;
	NonBlockingReader reader;
	PrintWriter writer;
	Console con = null;
	AddressRange range;

	public ConsoleDevice(PBus SystemBus, AddressRange range) {
		sysBus = SystemBus;
		this.range = range;
		try {
			terminal = TerminalBuilder.builder().jna(true).system(true).build();
			reader = terminal.reader();
			writer = terminal.writer();
			terminal.enterRawMode();
		} catch (IOException e) {

			e.printStackTrace();
			// raw mode means we get each key press rather
			// than line buffered input
		}

	}

	public void write(int data) throws DeviceUnavailable {
		// TODO Auto-generated method stub

		writer.append((char) data);

	}

	public void status(int data) throws DeviceUnavailable {
		// TODO Auto-generated method stub
		status = data;
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
			write(data);
		} else {
			logger.debug("Control   : " + data);
			control(data);
		}

	}

	public byte read(int address) throws illegalAddressException, DeviceUnavailable {
		// TODO Auto-generated method stub
		byte returnValue = 0;
		if ((address & 0x0001) == 0) {
			returnValue = (byte) read(); // get character from console
		} else {

			returnValue = (byte) status(); // get status flags of the console
		}
		return returnValue;
//		if ((address & 0x0001) == 0) {
//			logger.debug("Reading from file ");
//			try {
//				char[] cbuff = new char[10];
//				
//				int chsread = 0;
//				while ( chsread <= 0 ) {
//					chsread = System.console().reader().read(cbuff,0, 1);
//					Thread.sleep(10);
//				}
//				returnValue =  (byte) cbuff[0];
//			} catch (IOException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			} catch (InterruptedException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//		} 
//		return returnValue;
	}

	public void raiseInterrupt() {
		// TODO Auto-generated method stub
		sysBus.raiseInterupt();
	}

	@Override
	public int read() throws DeviceUnavailable {
		// TODO Auto-generated method stub
		if (input.isEmpty()) {
			return 0;
		} else {
			Integer i = input.remove();
			return (byte) (i.byteValue() & 0x00ff);
		}

	}

	@Override
	public int status() throws DeviceUnavailable {
		// TODO Auto-generated method stub
		return (byte) status;
	}

	@Override
	public void control(int data) throws DeviceUnavailable {
		status = (byte) (data & 0x00ff);

	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
		boolean run = true;
		while (run && !Thread.currentThread().isInterrupted()) {

			int peekresults = 0;
			

		try {
					peekresults = reader.peek(10);
					if (peekresults > 0) {
						int value = reader.read();
						Integer i;
						i = Integer.valueOf(value);
						input.add( i);
					}
				} catch (IOException e) {
					// TODO Auto-generated catch block
					
					run = false;
				}

			
		}
		logger.debug("Char Device stopped");
		
	}

	@Override
	public void reset() {
		// TODO Auto-generated method stub

	}

}
