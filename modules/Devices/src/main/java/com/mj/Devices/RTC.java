package com.mj.Devices;

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
	private AddressRange mr;
	// Big-endian u_time: seconds since the Unix epoch (UTC), updated once a second.
	// Calendar/display conversion is firmware's job, not this device's.
	private byte[] time;
	private InterruptLine interruptLine;

	/** interruptLine is whatever this machine's own interrupt mechanism is
	 * (a VIA6522's requestInterrupt(FLAG_T1) lambda on the 6502 side,
	 * matching the real hardware's own VIA-routed convention; a
	 * VectoredInterruptController's requestInterrupt() on the Z80 side) -
	 * only exercised if something actually calls start() on this Thread
	 * (see run()/read()'s own comments for why that's optional, not
	 * required, for a caller that just wants to poll a live value). range
	 * is this device's own 4-byte register block's address, which varies
	 * by machine (CPU_CreateUtil's scratch bus vs. the real CPUMain/
	 * Z80VideoDiskMachine wiring) - no longer hardcoded here. */
	public RTC(InterruptLine interruptLine, AddressRange range) {
		this.interruptLine = interruptLine;
		this.mr = range;
		this.time = new byte[mr.size()];
		now();
	}

	public DEVTYPE getDeviceType() {
		return DEVTYPE.BLOCK;
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
		int localAddress = address - mr.startAddressRange;
		if (localAddress == 0 && !isAlive()) {
			// Nobody ever started this device's own ticking thread (a plain
			// polling caller, like BIOS_GET_TIME, has no need to) - refresh
			// here instead, right as the first byte of a 4-byte read
			// begins, so all 4 bytes still come from one consistent
			// instant. Skipped once the thread IS running (see run()): it
			// already keeps `time` fresh on its own one-second schedule,
			// and refreshing here too would risk a torn read mid-copy for
			// whatever ISR is mid-way through draining these 4 bytes.
			now();
		}
		readLock.lock();
		byte value = time[localAddress];
		readLock.unlock();
		if (logger.isTraceEnabled()) {
			logger.trace(String.format("Address %04x Virtual Address %04x Value %02x", localAddress, address, value));
		}
		return value;
	}

	@Override
	public void raiseInterrupt() {
		// TODO Auto-generated method stub

	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
		
		while (!isInterrupted()) {
			now();
			interruptLine.raiseInterrupt();
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				return;	
			}

		}
	}

	public void now() {
		int epochSeconds = (int) (System.currentTimeMillis() / 1000L);
		readLock.lock();
		time[0] = (byte) (epochSeconds >>> 24);
		time[1] = (byte) (epochSeconds >>> 16);
		time[2] = (byte) (epochSeconds >>> 8);
		time[3] = (byte) epochSeconds;
		readLock.unlock();

		if (logger.isTraceEnabled()) {
			logger.trace(String.format("RTC-NOW: %08x", epochSeconds));
		}
	}

	@Override
	public void reset() {
		// TODO Auto-generated method stub
		
	}
}
