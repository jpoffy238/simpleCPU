package com.mj.Devices;

import java.util.List;

import com.mj.exceptions.DeviceUnavailable;
import com.mj.exceptions.ROException;
import com.mj.exceptions.illegalAddressException;

public interface PBus extends CPUBus {
	enum BussId {
		MEMROY, DEVICE, INTERUPT, CPU
	}

	enum DEVTYPE {
		BLOCK, CHAR
	}

	enum IOALLOW {
		RO, RW, WO
	}
	public void startDevices();
	public void stopDevices();
	public void write(BussId bus, int address, byte data)
			throws ROException, illegalAddressException, DeviceUnavailable;

	public byte read(BussId bus, int destAddress) throws illegalAddressException, DeviceUnavailable;

	public void blockWrite(BussId bus, int address, byte data[])
			throws ROException, illegalAddressException, DeviceUnavailable;

	public byte[] blockRead(BussId bus, int destAddress) throws illegalAddressException, DeviceUnavailable;

	public void resetDevice();
	
	public void raiseInterupt();

	public void raiseNMInterupt();

	public void raisepowerOnReset();

	public void clearInterupt();

	public void clearNMInterupt();

	public void clearpowerOnReset();

	public void registerDevice(Device deviceHandler);

	public void unregisterDevice(Device deviceHandler);

	public List <Device> getDevices();

	/** Registers a real Z80 IM2 vector supplier (see InterruptVectorSource's
	 * own comment) - in priority order, first registered highest, the same
	 * "closest to the CPU wins" convention a real daisy chain uses. Separate
	 * from registerDevice(): a vector source is also usually a Device (for
	 * its own port I/O, like VectoredInterruptController's vector register),
	 * but the two registrations are independent - this one is only ever
	 * consulted by acknowledgePendingVector(), never by read()/write(). */
	public void registerInterruptVectorSource(InterruptVectorSource source);

	/** Scans registered sources in priority order and returns the first one
	 * with hasPendingInterrupt() true, acknowledging it (see
	 * InterruptVectorSource.acknowledge()) as a side effect - or -1 if none
	 * currently have anything pending. */
	public int acknowledgePendingVector();

}
