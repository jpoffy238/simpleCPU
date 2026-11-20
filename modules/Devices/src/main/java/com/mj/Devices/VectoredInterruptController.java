package com.mj.Devices;

import com.mj.Devices.PBus.BussId;
import com.mj.Devices.PBus.DEVTYPE;
import com.mj.exceptions.DeviceUnavailable;
import com.mj.exceptions.ROException;
import com.mj.exceptions.illegalAddressException;

/** A minimal Z80 IM2 vectored-interrupt supplier - modeled after the real
 * family of Z80 peripherals (PIO, CTC) that actually held an interrupt-vector
 * register on period hardware, not a full emulation of either specific chip.
 *
 * A real floppy disk controller (FloppyController, this project's own) never
 * had a Z80-style vector register itself - on a real system its INTRQ line
 * was wired into a PIO or CTC chip's own interrupt-request input, and THAT
 * chip is what actually sat in the Z80's IM2 daisy chain and supplied the
 * vector byte during the interrupt-acknowledge cycle. This class plays that
 * role: other devices raise a plain interrupt through it (an InterruptLine,
 * same as they'd wire to any other device, via requestInterrupt() below) -
 * same as real hardware, they know nothing about vectors at all. Software
 * loads the vector register itself via an ordinary port write, the real Z80
 * PIO/CTC convention (any byte written to the control port becomes the new
 * vector) - deliberately not the full real control-byte protocol either chip
 * actually uses to multiplex several configuration purposes onto one port,
 * since this project has no other configuration to multiplex: every write
 * here is always a vector load, no mode bit needed to say so.
 *
 * Deliberately single-channel - this project has exactly one real interrupt-
 * raising device today (FloppyController). A second device wanting its own
 * vector would need a second instance (a second port address) or a real
 * multi-channel redesign; not built ahead of that actually being needed. */
public class VectoredInterruptController implements Device, InterruptVectorSource {
	public static final int REG_VECTOR = 0;

	private final PBus bus;
	private final AddressRange range;

	private int vector = 0;
	private boolean pending;

	public VectoredInterruptController(PBus bus, AddressRange range) {
		this.bus = bus;
		this.range = range;
		bus.registerInterruptVectorSource(this);
	}

	/** Wired as an InterruptLine into whatever device needs to request an
	 * interrupt through this controller (today, FloppyController) - same
	 * shape every other interrupt-raising device already uses, so that
	 * device needs no change and no awareness that a vector is involved at
	 * all. */
	public void requestInterrupt() {
		pending = true;
		bus.raiseInterupt();
	}

	@Override
	public DEVTYPE getDeviceType() {
		return DEVTYPE.CHAR;
	}

	@Override
	public BussId getBusId() {
		return BussId.DEVICE;
	}

	@Override
	public AddressRange getAddressRange() {
		return range;
	}

	@Override
	public void write(int address, byte data) throws illegalAddressException, ROException, DeviceUnavailable {
		range.addressMapper(address); // validates address; REG_VECTOR is this controller's only register
		vector = data & 0xff;
	}

	@Override
	public byte read(int address) throws illegalAddressException, DeviceUnavailable {
		range.addressMapper(address);
		return (byte) vector;
	}

	@Override
	public boolean hasPendingInterrupt() {
		return pending;
	}

	@Override
	public int vector() {
		return vector;
	}

	@Override
	public void acknowledge() {
		pending = false;
	}

	@Override
	public void raiseInterrupt() {
		// Device's own interface method - nothing in this project actually
		// calls it (same as VideoController's own no-op; see its comment).
		// Other devices reach this controller through requestInterrupt()
		// instead, which is what real wiring (Z80VideoDiskMachine) uses.
	}

	@Override
	public void reset() {
		vector = 0;
		pending = false;
	}
}
