package com.mj.Devices;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.mj.Devices.PBus.BussId;
import com.mj.Devices.PBus.DEVTYPE;
import com.mj.exceptions.DeviceUnavailable;
import com.mj.exceptions.ROException;
import com.mj.exceptions.illegalAddressException;

/** A MOS 6522 VIA. Every device that wants to interrupt the CPU goes through
 * here rather than touching the bus's shared IRQ line directly - the VIA is
 * the single point that aggregates sources (one bit each in IFR/IER) onto
 * that one line, same as real hardware.
 *
 * Only the interrupt subsystem (IFR/IER) is functional. The ports, timers and
 * shift register occupy their real addresses so nothing has to be renumbered
 * later, but for now they're just inert storage.
 *
 * Register map (offset from base):
 *   0x0 ORB   0x1 ORA   0x2 DDRB  0x3 DDRA
 *   0x4 T1C-L 0x5 T1C-H 0x6 T1L-L 0x7 T1L-H
 *   0x8 T2C-L 0x9 T2C-H 0xA SR    0xB ACR
 *   0xC PCR   0xD IFR   0xE IER   0xF ORA (no handshake)
 *
 * IFR/IER bit assignments (bits 0-6; bit 7 is special on both registers):
 *   0 CA2   1 CA1   2 SR   3 CB2   4 CB1   5 T2   6 T1
 * IFR bit 7 (read-only) is the OR of (IFR & IER) bits 0-6 - the actual signal
 * that drives the CPU's shared IRQ line. Writing a 1 to an IFR bit clears
 * that flag. IER bit 7 selects set-mode (1) or clear-mode (0) for whichever
 * of bits 0-6 are written as 1; reading IER always reads bit 7 as 1. */
public class VIA6522 implements Device {
	public static final int REG_ORB = 0x0;
	public static final int REG_ORA = 0x1;
	public static final int REG_DDRB = 0x2;
	public static final int REG_DDRA = 0x3;
	public static final int REG_T1C_L = 0x4;
	public static final int REG_T1C_H = 0x5;
	public static final int REG_T1L_L = 0x6;
	public static final int REG_T1L_H = 0x7;
	public static final int REG_T2C_L = 0x8;
	public static final int REG_T2C_H = 0x9;
	public static final int REG_SR = 0xA;
	public static final int REG_ACR = 0xB;
	public static final int REG_PCR = 0xC;
	public static final int REG_IFR = 0xD;
	public static final int REG_IER = 0xE;
	public static final int REG_ORA_NO_HANDSHAKE = 0xF;

	public static final int FLAG_CA2 = 0x01;
	public static final int FLAG_CA1 = 0x02;
	public static final int FLAG_SR = 0x04;
	public static final int FLAG_CB2 = 0x08;
	public static final int FLAG_CB1 = 0x10;
	public static final int FLAG_T2 = 0x20;
	public static final int FLAG_T1 = 0x40;
	public static final int FLAG_IRQ = 0x80;

	private static final int FLAG_MASK = 0x7f;

	private final Lock lock = new ReentrantLock();
	private final PBus bus;
	private final AddressRange range;
	private final int[] inertRegisters = new int[16];

	private int ifr = 0;
	private int ier = 0;

	public VIA6522(PBus bus, AddressRange range) {
		this.bus = bus;
		this.range = range;
	}

	public DEVTYPE getDeviceType() {
		return DEVTYPE.CHAR;
	}

	public BussId getBusId() {
		return BussId.DEVICE;
	}

	public AddressRange getAddressRange() {
		return range;
	}

	/** Called by other devices (from their own threads) to signal the CPU.
	 * flagBit is one of the FLAG_* constants above. */
	public void requestInterrupt(int flagBit) {
		lock.lock();
		try {
			ifr |= (flagBit & FLAG_MASK);
			updateIrqLine();
		} finally {
			lock.unlock();
		}
	}

	@Override
	public void write(int address, byte data) throws illegalAddressException, ROException, DeviceUnavailable {
		int reg = range.addressMapper(address);
		int value = data & 0xff;
		lock.lock();
		try {
			switch (reg) {
			case REG_IFR:
				ifr &= ~(value & FLAG_MASK);
				updateIrqLine();
				break;
			case REG_IER:
				if ((value & 0x80) != 0) {
					ier |= (value & FLAG_MASK);
				} else {
					ier &= ~(value & FLAG_MASK);
				}
				updateIrqLine();
				break;
			default:
				inertRegisters[reg] = value;
			}
		} finally {
			lock.unlock();
		}
	}

	@Override
	public byte read(int address) throws illegalAddressException, DeviceUnavailable {
		int reg = range.addressMapper(address);
		lock.lock();
		try {
			switch (reg) {
			case REG_IFR:
				int irqBit = ((ifr & ier & FLAG_MASK) != 0) ? FLAG_IRQ : 0;
				return (byte) (ifr | irqBit);
			case REG_IER:
				return (byte) (ier | 0x80);
			default:
				return (byte) inertRegisters[reg];
			}
		} finally {
			lock.unlock();
		}
	}

	private void updateIrqLine() {
		if ((ifr & ier & FLAG_MASK) != 0) {
			bus.raiseInterupt();
		} else {
			bus.clearInterupt();
		}
	}

	@Override
	public void raiseInterrupt() {
		lock.lock();
		try {
			updateIrqLine();
		} finally {
			lock.unlock();
		}
	}

	@Override
	public void reset() {
		lock.lock();
		try {
			ifr = 0;
			ier = 0;
			for (int i = 0; i < inertRegisters.length; i++) {
				inertRegisters[i] = 0;
			}
		} finally {
			lock.unlock();
		}
	}
}
